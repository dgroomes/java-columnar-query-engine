package dgroomes.queryengine;

import dgroomes.queryapi.Query;
import dgroomes.queryengine.Column.IntegerColumn;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

/**
 * This is a toy pattern matching query engine (like the 'cypher' query language). It does not do aggregations.
 * There is no query "language" and instead it's just a Java API.
 */
public class Executor {

    public final Verifier verifier;

    public Executor() {
        verifier = new Verifier();
    }

    /**
     * Given a query (should the query encapsulate the data source? answer: probably not the data source but probably the
     * schema?), return a list of matching records.
     * <p>
     * Decision: This method returns the query result set as a "value". In other words, this method does not return a
     * "view" of the result set and it does not return a cursor over the result set. This is a conscious decision. This
     * design allows for a less complex implementation. I'm more interested in spending my time learning concepts with
     * this project than I am interested in maximizing user features/options.
     * <p>
     * Tentative decision: this returns a pruned table. Consider a table that represents a city. It has two columns:
     * "city_name" and "state code". Return a new table where its component columns are pruned down to the entries that
     * matched the criteria.
     * <p>
     * Tentative decision: it's ok to not parse criteria like "x < 5" and instead just use lambda. In this way, we can't
     * do a whole category of optimization but I don't care to do that.
     * <p>
     * Decision: full scans are good. I don't care about indexing. This data is in-memory and we want to support criteria
     * like regex which can't take advantage of indexes anyway.
     * <p>
     * I don't care much about generics here. I just want to get something working.
     */
    public QueryResult match(Query query, Table table) {
        Objects.requireNonNull(query, "The 'query' argument must not be null");
        Objects.requireNonNull(table, "The 'table' argument must not be null");

        Verifier.VerificationResult verificationResult = verifier.verify(query, table);

        ExecutionContext executionContext;
        if (verificationResult instanceof Verifier.VerificationResult.IllegalQuery(var message)) {
            return new QueryResult.Failure(message);
        } else if (verificationResult instanceof Verifier.VerificationResult.LegalQuery legalQuery) {
            executionContext = legalQuery.executionContext();
        } else {
            throw new IllegalStateException("Unexpected verification result: " + verificationResult);
        }

        // Algorithm working notes. We need to "prune from the leaves". Visit each leaf node, apply the scalar criterion
        // to filter down the data, and then prune the parent table by the associations, and repeat until we reach the
        // root. Do this for each leaf.
        Deque<ExecutionContext.Node> leaves = new ArrayDeque<>();

        // Let's find the leaf nodes
        Deque<ExecutionContext.Node> nodes = new ArrayDeque<>();
        nodes.push(executionContext.rootNode);
        while (!nodes.isEmpty()) {
            ExecutionContext.Node node = nodes.pop();
            List<ExecutionContext.Node> childNodes = node.childNodes();
            if (childNodes.isEmpty()) {
                leaves.push(node);
            } else {
                nodes.addAll(childNodes);
            }
        }

        while (!leaves.isEmpty()) {
            ExecutionContext.Node leaf = leaves.pop();
            leaf.filter();
            leaf.filterParent();
            ExecutionContext.Node parent = leaf.parent;
            if (parent != null) leaves.push(parent);
        }

        return toResultSet(table, executionContext.matchingIndices());
    }

    /**
     * Prune the table down to the rows at the matching indices This represents the final "result set" of the query.
     * <p>
     * This is designed to be used after evaluating the query criteria where we have identified a set of rows (by index)
     * that matched the criteria.
     */
    private QueryResult.Success toResultSet(Table table, int[] indices) {
        List<Column> prunedColumns = table.columns().stream()
                .map(column -> switch (column) {
                    case Column.BooleanColumn(var bools) -> {
                        var pruned = new boolean[indices.length];
                        for (int i = 0; i < indices.length; i++) {
                            pruned[i] = bools[indices[i]];
                        }
                        yield new Column.BooleanColumn(pruned);
                    }
                    case IntegerColumn(var ints) -> {
                        var pruned = new int[indices.length];
                        for (int i = 0; i < indices.length; i++) {
                            pruned[i] = ints[indices[i]];
                        }
                        yield new IntegerColumn(pruned);
                    }
                    case Column.StringColumn(var strings) -> {
                        var pruned = new String[indices.length];
                        for (int i = 0; i < indices.length; i++) {
                            pruned[i] = strings[indices[i]];
                        }
                        yield new Column.StringColumn(pruned);
                    }
                    case Column.AssociationColumn associationColumn -> {
                        var associations = associationColumn.associations;
                        var pruned = new Association[indices.length];
                        for (int i = 0; i < indices.length; i++) {
                            pruned[i] = associations[indices[i]];
                        }
                        yield new Column.AssociationColumn(associationColumn.associatedEntity, pruned);
                    }
                })
                .toList();

        return new QueryResult.Success(new Table(prunedColumns));
    }

    public sealed interface QueryResult permits QueryResult.Success, QueryResult.Failure {
        record Success(Table resultSet) implements QueryResult {
        }

        record Failure(String message) implements QueryResult {
        }
    }
}
