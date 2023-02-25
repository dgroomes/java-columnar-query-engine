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

  non-sealed interface SingleFieldIntegerQuery extends Query, IntCriteria {
  }

  record OrdinalSingleFieldIntegerQuery(int ordinal, IntCriteria intCriteria) implements Query {
  }
}
