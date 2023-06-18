package dgroomes.queryengine;

import dgroomes.datamodel.Column;
import dgroomes.datamodel.Table;
import dgroomes.queryapi.Criteria;
import dgroomes.queryapi.Query;

import java.util.*;

/**
 * A query verifier (or at least, my naive guess at what a query verifier is. Is this a linker? A compiler?).
 * <p>
 * We want to take a {@link dgroomes.queryapi.Criteria} and create an "execution context" from it. This is a
 * graph that is a physical representation of the query execution process. It incorporates tables as nodes.
 */
public class Verifier {

    public sealed interface VerificationResult {

        record LegalQuery(ExecutionContext executionContext) implements VerificationResult {}

        record IllegalQuery(String message) implements VerificationResult {}
    }

    /**
     * Verify if the given {@link Criteria} is legal for the given {@link Table}. If the criteria describes columns that
     * don't exist or types that don't match, then the query is illegal.
     *
     * @param query the query to verify and link
     * @param table The table to verify the criteria against
     * @return a {@link VerificationResult} that indicates if the query is legal or not. For legal queries, the result
     * will contain an {@link ExecutionContext} that can be used to execute the query.
     */
    public VerificationResult verify(Query query, Table table) {
        Objects.requireNonNull(query, "The 'query' argument must not be null");
        Objects.requireNonNull(table, "The 'table' argument must not be null");

        var executionContext = new ExecutionContext(table);

        // Algorithm working notes. We need to descend the query nodes and its child nodes, all the while verifying
        // that ordinals are "legal", meaning they follow columns that exist and are the right type. We turn each
        // query node into an execution node.
        record NodeNode(Query.Node queryNode, ExecutionContext.Node executionNode) {}
        Deque<NodeNode> toVisit = new ArrayDeque<>();
        toVisit.add(new NodeNode(query.rootNode, executionContext.rootNode));

        while (!toVisit.isEmpty()) {
            var nodeNode = toVisit.pop();
            var currentQueryNode = nodeNode.queryNode;
            var currentExecutionNode = nodeNode.executionNode;

            // Wire up the criteria.
            for (var criterion : currentQueryNode.getCriteria()) {
                int ordinal = criterion.ordinal();

                if (currentExecutionNode.table.columns().size() < ordinal) {
                    var msg = "The query ordinal '%d' is out of bounds for the table with %d columns".formatted(ordinal, currentExecutionNode.table.columns().size());
                    return new VerificationResult.IllegalQuery(msg);
                }

                Column column = currentExecutionNode.table.columns().get(ordinal);
                switch (column) {
                    case Column.StringColumn stringColumn -> {
                        if (!(criterion instanceof Criteria.StringCriteria stringCriteria))
                            return new VerificationResult.IllegalQuery("The column is a string column but the criterion is not a string predicate.");
                        currentExecutionNode.addColumnPredicate(idx -> stringCriteria.stringPredicate().test(stringColumn.strings()[idx]));
                    }
                    case Column.IntegerColumn intColumn -> {
                        if (!(criterion instanceof Criteria.IntCriteria intCriteria))
                            return new VerificationResult.IllegalQuery("The column is an integer column but the criterion is not an integer predicate.");
                        currentExecutionNode.addColumnPredicate(idx -> intCriteria.integerPredicate().test(intColumn.ints()[idx]));
                    }
                    case Column.BooleanColumn ignored -> {
                        return new VerificationResult.IllegalQuery("Boolean columns are not supported yet.");
                    }
                    case Column.AssociationColumn ignored -> {
                        return new VerificationResult.IllegalQuery("Association columns can't be matched on with a scalar criteria.");
                    }
                    case default -> throw new IllegalStateException("Unrecognized column type: %s. This is unexpected.".formatted(column.getClass().getName()));
                }
            }

            Map<Integer, Query.Node> childQueryNodesByOrdinal = currentQueryNode.getChildrenByOrdinal();

            for (Map.Entry<Integer, Query.Node> entry : childQueryNodesByOrdinal.entrySet()) {
                var ordinal = entry.getKey();
                var queryNode = entry.getValue();
                Column column = currentExecutionNode.table.columns().get(ordinal);

                if (!(column instanceof Column.AssociationColumn associationColumn)) {
                    return new VerificationResult.IllegalQuery("The column at ordinal %d is not an association column. It is a %s".formatted(ordinal, column.getClass().getName()));
                }
                var childNode = currentExecutionNode.createChildNode(associationColumn);
                toVisit.add(new NodeNode(queryNode, childNode));
            }
        }

        return new VerificationResult.LegalQuery(executionContext);
    }
}
