package dgroomes.datasystem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A toy query API. Instances of {@link Query} expresses a query over some structure of data. This is a
 * *definitional* concept. It does not couple to any particular physical data source.
 * <p>
 * The {@link @Query} is a graph.
 * <p>
 * Design language decision: the "query" refers to the top-level thing and "criteria" refers to the matching logic like
 * "x > 0". A query contains criteria. This is a nice one-way constrained relationship.
 */
public class Query {

    public final Node rootNode;
    public final String tableName;

    public Query(String tableName) {
        this.tableName = tableName;
        this.rootNode = new Node();
    }

    public static class Node {
        private final Map<Integer, Node> childrenByOrdinal = new HashMap<>();
        private final List<Criteria> criteria = new ArrayList<>();

        public Node createChild(int ordinal) {
            Node child = new Node();
            if (childrenByOrdinal.containsKey(ordinal)) {
                throw new IllegalArgumentException("A child already exists at ordinal " + ordinal);
            }

            childrenByOrdinal.put(ordinal, child);
            return child;
        }

        public Map<Integer, Node> getChildrenByOrdinal() {
            return Map.copyOf(childrenByOrdinal);
        }

        public Node addCriteria(Criteria criteria) {
            this.criteria.add(criteria);
            return this;
        }

        public List<Criteria> getCriteria() {
            return criteria;
        }
    }
}
