package dgroomes.datamodel;

/**
 * This is a core type. The {@link Column} type represents a column in a table.
 */
public interface Column {

    /**
     * The number of elements in the column.
     */
    int height();

    /**
     * The type of the column.
     */
    ColumnFilterable filterableType();
}
