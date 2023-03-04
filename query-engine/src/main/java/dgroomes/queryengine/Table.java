package dgroomes.queryengine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.IntStream;

/**
 * This is a key type. The {@link Table} type represents a physical table of rows and columns.
 * <p>
 * Note: I'm using a record type here but I might need an interface at some point. I don't need this be especially
 * abstract because I don't think I care to have different implementations of this type. The {@link Column}
 * type does more heavy lifting.
 * <p>
 * The words "field" and "column" are generally synonymous. For the design of this software, we'll make a clear
 * distinction: "field" will be used in definitional contexts like the way we use the word "schema"; "column" will be
 * used in physical contexts like when we are talking about a physical array of values.
 * <p>
 * I'm facing a design challenge: do the columns need names? Technically we can use the ordinal position of the columns
 * as identity and we don't need to name them. After all, names are for human readability. Naming should be used in the
 * query language, like in a "Schema" type?
 */
public record Table(List<Column> columns) {

  /**
   * Convenience method for creating an {@link Column.IntegerColumn} for toy examples, like in test cases.
   */
  public static Table ofColumns(Column... columns) {
    var mutableList = new ArrayList<>(Arrays.asList(columns));
    return new Table(mutableList);
  }

  /**
   * Associate this entity (X) to another entity (Y).
   *
   * @param associatedEntity the entity type to associate to (Y)
   * @param associations     the associations from this entity (X) to the associated entity (Y)
   */
  void associateTo(Table associatedEntity, Association... associations) {
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

  public int size() {
    // This implementation is silly.
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
  public Table prune(int[] indices) {

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

    return new Table(new ArrayList<>(prunedColumns));
  }
}