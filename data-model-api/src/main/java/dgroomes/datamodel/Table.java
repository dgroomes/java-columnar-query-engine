package dgroomes.datamodel;

import java.util.List;

/**
 * This is a key type. The {@link Table} type represents the familiar "rows and columns" data model.
 * <p>
 * I'm facing a design challenge: do the columns need names? Technically we can use the ordinal position of the columns
 * as identity and we don't need to name them. After all, names are for human readability. Naming should be used in the
 * query language, like in a "Schema" type? UPDATE: yes I reinforce this idea that the core query types code to ordinal
 * positions. If we want to add names, I recommend taking a holistic approach and adding a whole plugin system for
 * observability via instrumentation. But again, not a main concern of this project.
 */
public interface Table {

    List<? extends Column> columns();

    /**
     * The 'width' is the number of columns.
     */
    default int width() {
        return columns().size();
    }

    /**
     * The 'size' is the number of rows in the table.
     */
    int size();

    /**
     * Produce a {@link Table} that is a subset of the current table given by the indices.
     */
    Table subset(int[] indices);
}
