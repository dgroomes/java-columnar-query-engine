package dgroomes.queryengine;

/**
 * WIP.
 * <p>
 * Expresses a query over some structure of data. This is a *definitional* concept. It does not couple to any particular
 * data source. See {@link ObjectGraph}.
 * <p>
 * Design language decision: the "query" refers to the top-level thing and "criteria" refers to the matching logic like
 * "x > 0". A query contains criteria. This is a nice one-way constrained relationship.
 */
public sealed interface Query {

  interface IntCriteria {
    boolean match(int integerUnderTest);
  }

  interface StringCriteria {
    boolean match(String stringUnderTest);
  }

  non-sealed interface SingleFieldIntegerQuery extends Query, IntCriteria {
  }

  record OrdinalSingleFieldIntegerQuery(int ordinal, IntCriteria intCriteria) implements Query {
  }

  // note: most of these interfaces/records are temporary. more comprehensive designs will surface.
  // This is a single-field query directed by a "pointer" to the field-under-test.
  record PointerSingleFieldStringQuery(Pointer pointer, StringCriteria stringCriteria) implements Query {}

  sealed interface Pointer {
    record Ordinal(int ordinal) implements Pointer {}

    // This is a nested pointer. This is the unit of recursion.
    record NestedPointer(int ordinal, Pointer pointer) implements Pointer {}
  }
}
