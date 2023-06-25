package dgroomes.data_system;

import java.util.function.IntPredicate;
import java.util.function.Predicate;

/**
 * A criteria describes a specification (like 'x > 0') that targets values in a column. The column is not referenced
 * physically but is instead described by its ordinal pointer.
 */
sealed public interface Criteria permits Criteria.IntCriteria, Criteria.StringCriteria {

    /**
     * The ordinal of the column in the table.
     */
    int ordinal();

    record StringCriteria(int ordinal, Predicate<String> stringPredicate) implements Criteria {}

    record IntCriteria(int ordinal, IntPredicate integerPredicate) implements Criteria {}
}
