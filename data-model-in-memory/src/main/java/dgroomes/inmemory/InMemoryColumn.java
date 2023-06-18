package dgroomes.inmemory;

import dgroomes.datamodel.Association;
import dgroomes.datamodel.Column;
import dgroomes.datamodel.ColumnFilterable;
import dgroomes.datamodel.Table;

import java.util.function.IntPredicate;
import java.util.function.Predicate;

/**
 * A {@link Column} implementation that is backed by an in-memory data structure.
 * <p>
 * Note: for a toy query engine, we can get away with using a minimal set of column types. We cover three points on the
 * data spectrum: boolean (single bit), integer (32 bits), and string (variable length). Covering other types would be
 * redundant for learning, but they would be needed in a real/useful implementation.
 */
sealed public interface InMemoryColumn extends Column {

    /**
     * Convenience method for creating an {@link IntegerColumn} for toy examples, like in test cases.
     */
    static IntegerColumn ofInts(int... ints) {
        return new IntegerColumn(ints);
    }

    static StringColumn ofStrings(String... strings) {
        return new StringColumn(strings);
    }

    record BooleanColumn(boolean[] bools) implements InMemoryColumn, ColumnFilterable.BooleanColumnFilterable {

        @Override
        public ColumnFilterable filterableType() {
            return this;
        }

        @Override
        public IntPredicate where(Predicate<Boolean> predicate) {
            throw new IllegalStateException("not implemented");
        }

        @Override
        public int height() {
            return bools.length;
        }
    }

    record IntegerColumn(int[] ints) implements InMemoryColumn, ColumnFilterable.IntegerColumnFilterable {

        @Override
        public ColumnFilterable filterableType() {
            return this;
        }

        @Override
        public IntPredicate where(IntPredicate predicate) {
            return idx -> predicate.test(ints[idx]);
        }

        @Override
        public int height() {
            return ints.length;
        }
    }

    record StringColumn(String[] strings) implements InMemoryColumn, ColumnFilterable.StringColumnFilterable {

        @Override
        public ColumnFilterable filterableType() {
            return this;
        }

        @Override
        public IntPredicate where(Predicate<String> predicate) {
            return idx -> predicate.test(strings[idx]);
        }

        @Override
        public int height() {
            return strings.length;
        }
    }

    // Note: maybe modelling an association as a column of the entity is a bad idea. After all, the association is
    // usually goes both ways (bi-directional) in meaning. For example, a city is contained in a state and that state
    // also contains the city. There is a case for uni-directional associations, but I'm not there right now.
    final class AssociationColumn implements InMemoryColumn, ColumnFilterable.AssociationColumnFilterable {

        public final Table associatedEntity;
        public final Association[] associations;

        @Override
        public ColumnFilterable filterableType() {
            return this;
        }

        @Override
        public IntPredicate where(Predicate<Association> predicate) {
            throw new IllegalStateException("not implemented");
        }

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
