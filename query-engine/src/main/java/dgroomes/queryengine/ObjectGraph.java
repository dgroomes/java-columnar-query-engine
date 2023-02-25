package dgroomes.queryengine;

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

  /**
   * Convenience method for creating an {@link Column.IntegerColumn} for toy examples, like in test cases.
   */
  static MultiColumnEntity of(Column... columns) {
    return new MultiColumnEntity(List.of(columns));
  }

  sealed interface Column {
    record BooleanColumn(boolean[] bools) implements Column, ObjectGraph {}

    record IntegerColumn(int[] ints) implements Column, ObjectGraph {}

    record StringColumn(String[] strings) implements Column, ObjectGraph {}
  }

  record MultiColumnEntity(List<? extends Column> columns) implements ObjectGraph {

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
