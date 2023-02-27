package dgroomes.queryengine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.IntStream;

/**
 * WORK IN PROGRESS
 * <p>
 * Represents a concrete graph of columnar data.
 * <p>
 * The words "field" and "column" are generally synonymous. For the design of this software, we'll make a clear
 * distinction: "field" will be used in definitional contexts like the way we use the word "schema"; "column" will be
 * used in physical contexts like when we are talking about a physical array of values.
 * <p>
 * I'm facing a design challenge: do the columns need names? Technically we can use the ordinal position of the columns
 * as identity and we don't need to name them. After all, names are for human readability. Naming should be used in the
 * query language, like in a "Schema" type?
 * <p>
 * Future problem: how to implement joins/associations? That's a hard problem for a future me.
 */
public sealed interface ObjectGraph {

  /**
   * Convenience method for creating an {@link Column.IntegerColumn} for toy examples, like in test cases.
   */
  static Column.IntegerColumn ofInts(int... ints) {
    return new Column.IntegerColumn(ints);
  }

  static Column.StringColumn ofStrings(String... strings) {
    return new Column.StringColumn(strings);
  }

  /**
   * Convenience method for creating an {@link Column.IntegerColumn} for toy examples, like in test cases.
   */
  static MultiColumnEntity ofColumns(Column... columns) {
    var mutableList = new ArrayList<>(Arrays.asList(columns));
    return new MultiColumnEntity(mutableList);
  }

  static Association.None toNone() {
    return Association.None.NONE;
  }

  static Association.One toOne(int idx) {
    return new Association.One(idx);
  }

  static Association.Many toMany(int... indices) {
    return new Association.Many(indices);
  }

  sealed interface Association {
    None NONE = new None();

    final class None implements Association {
      private None() {
      }
    }

    record One(int idx) implements Association {}

    record Many(int[] indices) implements Association {}
  }

  /**
   * Note: for a toy query engine, we can get away with using a minimal set of column types. We cover three points on the
   * data spectrum: boolean (single bit), integer (32 bits), and string (variable length). Covering other types would be
   * redundant for learning but they would be needed in a real/useful implementation.
   */
  sealed interface Column {

    record BooleanColumn(boolean[] bools) implements Column, ObjectGraph {}

    record IntegerColumn(int[] ints) implements Column, ObjectGraph {}

    record StringColumn(String[] strings) implements Column, ObjectGraph {}

    // Note: maybe modelling an association as a column of the entity is a bad idea. After all, the association is
    // usually goes both ways (bi-directional) in meaning. For example, a city is contained in a state and that state
    // also contains the city. There is a case for uni-directional associations, but I'm not there right now.
    final class AssociationColumn implements Column {

      public final MultiColumnEntity associatedEntity;
      public final Association[] associations;

      // The dreaded bootstrapping problem with cyclic data structures. This has to be initialized later than the
      // constructor and this is why this class can't be a record.
      private AssociationColumn reverseAssociatedColumn;

      public AssociationColumn(MultiColumnEntity associatedEntity, Association[] associations) {
        this.associatedEntity = associatedEntity;
        this.associations = associations;
      }

      public void setReverseAssociatedColumn(AssociationColumn reverseAssociatedColumn) {
        if (this.reverseAssociatedColumn != null) {
          throw new IllegalStateException("reverseAssociatedColumn is already set");
        }
        this.reverseAssociatedColumn = reverseAssociatedColumn;
      }

      public AssociationColumn reverseAssociatedColumn() {
        if (reverseAssociatedColumn == null) {
          throw new IllegalStateException("reverseAssociatedColumn was never set");
        }
        return reverseAssociatedColumn;
      }
    }
  }

  record MultiColumnEntity(List<Column> columns) implements ObjectGraph {

    /**
     * Associate this entity (X) to another entity (Y).
     *
     * @param associatedEntity the entity type to associate to (Y)
     * @param associations     the associations from this entity (X) to the associated entity (Y)
     */
    void associateTo(MultiColumnEntity associatedEntity, Association... associations) {
      var associationColumn = new Column.AssociationColumn(associatedEntity, associations);
      // Note: yes this is nasty; we are using a mutable data structure. I love using records, but I often wind up with
      // a need to mutate it and struggle.
      this.columns.add(associationColumn);

      // Create a reverse association from Y to X.
      //
      // Note: this is some stream of consciousness code and needs to be cleaned up.
      Column.AssociationColumn reverseAssociationColumn;
      {
        var yIndexToXAssociations = new HashMap<Integer, List<Integer>>();
        for (int xIndex = 0; xIndex < associations.length; xIndex++) {
          int[] yIndices = switch (associations[xIndex]) {
            case Association.One(var idx) -> new int[]{idx};
            case Association.Many(var indices) -> indices;
            case Association.None ignored -> new int[]{};
          };

          for (int yIndex : yIndices) {
            var yToX = yIndexToXAssociations.computeIfAbsent(yIndex, ignored -> new ArrayList<>());
            yToX.add(xIndex);
          }
        }

        Association[] yToXAssociations = IntStream.range(0, associatedEntity.size()).mapToObj(yIndex -> {
          List<Integer> xIndices = yIndexToXAssociations.get(yIndex);
          return switch (xIndices.size()) {
            case 0 -> Association.None.NONE;
            case 1 -> new Association.One(xIndices.get(0));
            default -> new Association.Many(xIndices.stream().mapToInt(i -> i).toArray());
          };
        }).toArray(Association[]::new);
        reverseAssociationColumn = new Column.AssociationColumn(this, yToXAssociations);
        reverseAssociationColumn.setReverseAssociatedColumn(associationColumn);
        associationColumn.setReverseAssociatedColumn(reverseAssociationColumn);
      }

      associatedEntity.columns().add(reverseAssociationColumn);
    }

    // This is silly.
    private int size() {
      Column column = columns.get(0);
      return switch (column) {
        case Column.BooleanColumn boolColumn -> boolColumn.bools().length;
        case Column.IntegerColumn intColumn -> intColumn.ints().length;
        case Column.StringColumn stringColumn -> stringColumn.strings().length;
        case Column.AssociationColumn associationColumn -> associationColumn.associations.length;
      };
    }

    /**
     * Prune the multi-column entity down to the rows at the given indices. This is designed to be used after query
     * execution to prune down the result set from the elements (by index) that matched the search criteria.
     */
    public MultiColumnEntity prune(int[] indices) {

      // Clone each column and prune the data set down to the given indices
      // TODO clean this up.
      List<Column> prunedColumns = columns.stream()
              .<Column>map(column -> switch (column) {
                case Column.BooleanColumn boolColumn -> {
                  var bools = boolColumn.bools();
                  var pruned = new boolean[indices.length];
                  for (int i = 0; i < indices.length; i++) {
                    pruned[i] = bools[indices[i]];
                  }
                  yield new Column.BooleanColumn(pruned);
                }
                case Column.IntegerColumn intColumn -> {
                  var ints = intColumn.ints();
                  var pruned = new int[indices.length];
                  for (int i = 0; i < indices.length; i++) {
                    pruned[i] = ints[indices[i]];
                  }
                  yield new Column.IntegerColumn(pruned);
                }
                case Column.StringColumn stringColumn -> {
                  var strings = stringColumn.strings();
                  var pruned = new String[indices.length];
                  for (int i = 0; i < indices.length; i++) {
                    pruned[i] = strings[indices[i]];
                  }
                  yield new Column.StringColumn(pruned);
                }
                case Column.AssociationColumn associationColumn -> {
                  var associations = associationColumn.associations;
                  var pruned = new Association[indices.length];
                  for (int i = 0; i < indices.length; i++) {
                    pruned[i] = associations[indices[i]];
                  }
                  yield new Column.AssociationColumn(associationColumn.associatedEntity, pruned);
                }
              })
              .toList();

      return new MultiColumnEntity(new ArrayList<>(prunedColumns));
    }
  }
}
