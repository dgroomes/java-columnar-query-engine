package dgroomes.queryengine;

import dgroomes.datasystem.DataSystem;
import dgroomes.datasystem.Query;
import dgroomes.datasystem.QueryResult;
import dgroomes.datasystem.Table;

import java.util.*;

/**
 * An implementation of a {@link DataSystem} that is characterized by a serial execution strategy which relies on
 * tracking matching indices with {@link java.util.BitSet} data structures.
 */
public class DataSystemSerialIndices implements DataSystem {

    public final Verifier verifier;

    private final Map<String, Table> tables = new HashMap<>();

    public DataSystemSerialIndices() {
        verifier = new Verifier();
    }

    /**
     * Register a {@link Table} into the data system.
     */
    public void register(String tableName, Table table) {
        tables.put(tableName, table);
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
    @Override
    public QueryResult execute(Query query) {
        if (!tables.containsKey(query.tableName)) {
            var msg = "The query targets the table '%s' but that table is not registered".formatted(query.tableName);
            return new QueryResult.Failure(msg);
        }

        Table table = tables.get(query.tableName);

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

        // Let's find the leaf nodes. In the same loop, we also apply 'self' filtering on each node.
        Deque<ExecutionContext.Node> nodes = new ArrayDeque<>();
        nodes.push(executionContext.rootNode);
        while (!nodes.isEmpty()) {
            ExecutionContext.Node node = nodes.pop();
            node.filterSelf();
            List<ExecutionContext.Node> childNodes = node.childNodes();
            if (childNodes.isEmpty()) {
                leaves.push(node);
            } else {
                nodes.addAll(childNodes);
            }
        }

        // Filter upwards from the leaves, via associations.
        while (!leaves.isEmpty()) {
            ExecutionContext.Node leaf = leaves.pop();
            leaf.filterParent();
            ExecutionContext.Node parent = leaf.parent;
            if (parent != null) leaves.push(parent);
        }

        // Prune the table down to the rows at the matching indices This represents the final "result set" of the query.
        Table subset = table.subset(executionContext.matchingRows());
        return new QueryResult.Success(subset);
    }
}
