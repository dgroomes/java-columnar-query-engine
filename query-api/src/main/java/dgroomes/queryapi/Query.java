package dgroomes.queryapi;

import java.util.List;

/**
 * A toy query API. Instances of {@link Query} expresses a query over some structure of data. This is a
 * *definitional* concept. It does not couple to any particular physical data source.
 * <p>
 * Design language decision: the "query" refers to the top-level thing and "criteria" refers to the matching logic like
 * "x > 0". A query contains criteria. This is a nice one-way constrained relationship.
 */
public sealed interface Query {

  sealed interface Criteria permits IntCriteria, StringCriteria {}

  non-sealed interface IntCriteria extends Criteria {
    boolean match(int integerUnderTest);
  }

  non-sealed interface StringCriteria extends Criteria {
    boolean match(String stringUnderTest);
  }

  record OrdinalSingleFieldIntegerQuery(int ordinal, IntCriteria intCriteria) implements Query {
  }

  // Note: most of these interfaces/records are temporary. more comprehensive designs will surface.
  // This is a single-field query directed by a "pointer" to the field-under-test.
  record PointerSingleFieldStringQuery(Pointer pointer, StringCriteria stringCriteria) implements Query {}

  record PointedStringCriteriaQuery(List<PointedStringCriteria> pointedCriteriaList) implements Query {}

  record PointedStringCriteria(Pointer pointer, StringCriteria criteria) {}

  sealed interface Pointer {
    record Ordinal(int ordinal) implements Pointer {}

    // This is a nested pointer. This is the unit of recursion.
    record NestedPointer(int ordinal, Pointer pointer) implements Pointer {}
  }
}
