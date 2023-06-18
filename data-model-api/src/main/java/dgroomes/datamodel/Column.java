package dgroomes.datamodel;

/**
 * This is a core type. The {@link Column} type represents a physical column of data in a table.
 * <p>
 * Note: for a toy query engine, we can get away with using a minimal set of column types. We cover three points on the
 * data spectrum: boolean (single bit), integer (32 bits), and string (variable length). Covering other types would be
 * redundant for learning but they would be needed in a real/useful implementation.
 */
sealed public interface Column {

    /**
     * Convenience method for creating an {@link IntegerColumn} for toy examples, like in test cases.
     */
    static IntegerColumn ofInts(int... ints) {
        return new IntegerColumn(ints);
    }

    static StringColumn ofStrings(String... strings) {
        return new StringColumn(strings);
    }

    /**
     * The number of elements in the column.
     */
    int height();

    record BooleanColumn(boolean[] bools) implements Column {

        @Override
        public int height() {
            return bools.length;
        }
    }

    record IntegerColumn(int[] ints) implements Column {

        @Override
        public int height() {
            return ints.length;
        }
    }

    record StringColumn(String[] strings) implements Column {

        @Override
        public int height() {
            return strings.length;
        }
    }

    // Note: maybe modelling an association as a column of the entity is a bad idea. After all, the association is
    // usually goes both ways (bi-directional) in meaning. For example, a city is contained in a state and that state
    // also contains the city. There is a case for uni-directional associations, but I'm not there right now.
    final class AssociationColumn implements Column {

        public final Table associatedEntity;
        public final Association[] associations;

        @Override
        public int height() {
            return associations.length;
        }

        // The dreaded bootstrapping problem with cyclic data structures. This has to be initialized later than the
        // constructor and this is why this class can't be a record.
        private AssociationColumn reverseAssociatedColumn;

        public AssociationColumn(Table associatedEntity, Association[] associations) {
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
