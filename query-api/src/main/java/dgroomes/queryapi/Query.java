package dgroomes.queryapi;

import java.util.List;

/**
 * UPDATE: This abstraction isn't offering much right now when compared to {@link Criteria}. I'm going to stop using
 * this for now. Maybe I'll re-tool {@link Query} to represent an actual query plan/statistics/heuristics?\
 * <p>
 * A toy query API. Instances of {@link Query} expresses a query over some structure of data. This is a
 * *definitional* concept. It does not couple to any particular physical data source.
 * <p>
 * Design language decision: the "query" refers to the top-level thing and "criteria" refers to the matching logic like
 * "x > 0". A query contains criteria. This is a nice one-way constrained relationship.
 */
@Deprecated
public sealed interface Query {
    record CriteriaQuery(List<Criteria> criteria) implements Query {}
}
