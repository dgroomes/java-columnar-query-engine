package dgroomes.queryengine;

import java.util.ArrayList;
import java.util.List;

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
    return new MultiColumnEntity(List.of(columns));
  }

  static Association.None toNone() { return Association.None.NONE;}

  static Association.One toOne(int idx) { return new Association.One(idx);}

  static Association.Many toMany(int... indices) { return new Association.Many(indices);}

  sealed interface Association {
    None NONE = new None();
    final class None implements Association {
      private None() {}
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
    record AssociationColumn(ObjectGraph associatedEntity, Association[] associations) implements Column {}
  }

  record MultiColumnEntity(List<? extends Column> columns) implements ObjectGraph {

    MultiColumnEntity associateTo(ObjectGraph associatedEntity, Association... associations) {
      List<Column> newColumns =  new ArrayList<>(this.columns());
      var associationColumn = new Column.AssociationColumn(associatedEntity, associations);
      newColumns.add(associationColumn);
      return new MultiColumnEntity(newColumns);
    }

    /**
     * Prune the multi-column entity down to the rows at the given indices. This is designed to be used after query
     * execution to prune down the result set from the elements (by index) that matched the search criteria.
     */
    public MultiColumnEntity prune(int[] indices) {

      // Clone each column and prune the data set down to the given indices
      // TODO refactor this to use a visitor pattern or whatever.
      List<? extends Column> prunedColumns = columns.stream()
              .map(column -> {
                if (column instanceof Column.BooleanColumn boolColumn) {
                  var bools = boolColumn.bools();
                  var pruned = new boolean[indices.length];
                  for (int i = 0; i < indices.length; i++) {
                    pruned[i] = bools[indices[i]];
                  }
                  return new Column.BooleanColumn(pruned);
                } else if (column instanceof Column.IntegerColumn intColumn) {
                  var ints = intColumn.ints();
                  var pruned = new int[indices.length];
                  for (int i = 0; i < indices.length; i++) {
                    pruned[i] = ints[indices[i]];
                  }
                  return new Column.IntegerColumn(pruned);
                } else if (column instanceof Column.StringColumn stringColumn) {
                  var strings = stringColumn.strings();
                  var pruned = new String[indices.length];
                  for (int i = 0; i < indices.length; i++) {
                    pruned[i] = strings[indices[i]];
                  }
                  return new Column.StringColumn(pruned);
                } else {
                  throw new RuntimeException("Unexpected column type: " + column.getClass().getSimpleName());
                }
              })
              .toList();

      return new MultiColumnEntity(prunedColumns);
    }
  }
}
