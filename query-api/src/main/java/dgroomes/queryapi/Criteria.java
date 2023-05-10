package dgroomes.queryapi;

import java.util.function.Predicate;

/**
 * A criteria describes a specification (like 'x > 0') for a specific (e.g. "pointed at") column value.
 */
sealed public interface Criteria permits Criteria.PointedIntCriteria, Criteria.PointedStringCriteria {

  Pointer pointer();

  record PointedStringCriteria(Pointer pointer, Predicate<String> stringPredicate) implements Criteria {}

  record PointedIntCriteria(Pointer pointer, Predicate<Integer> integerPredicate) implements Criteria {}
}
