package dgroomes.datamodel;

import java.util.function.IntPredicate;
import java.util.function.Predicate;

/**
 * Implementations of this interface provide a means for defining a filter over a column using the given predicate.
 * The semantics of the filter is that "All rows that do not match the predicate will be removed from the result set."
 */
public sealed interface ColumnFilterable {

    non-sealed interface StringColumnFilterable extends ColumnFilterable {
        IntPredicate where(Predicate<String> predicate);
    }

    non-sealed interface IntegerColumnFilterable extends ColumnFilterable {
        IntPredicate where(IntPredicate predicate);
    }

    non-sealed interface BooleanColumnFilterable extends ColumnFilterable {
        IntPredicate where(Predicate<Boolean> predicate);
    }

    // Not sure this makes sense. Again, modelling associations as a column might be a bad idea.
    non-sealed interface AssociationColumnFilterable extends ColumnFilterable {
        IntPredicate where(Predicate<Association> predicate);
    }
}
