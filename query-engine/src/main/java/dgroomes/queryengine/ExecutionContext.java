package dgroomes.queryengine;

import dgroomes.datamodel.Association;
import dgroomes.datamodel.AssociationColumn;
import dgroomes.datamodel.Table;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Optional;
import java.util.function.IntPredicate;

/**
 * A verified and stateful representation of a query and its execution state.
 * <p>
 * This type comes after the {@link dgroomes.queryapi.Criteria} and before the {@link dgroomes.queryengine.Executor.QueryResult}.
 * I tried to get leverage from features like sealed classes, records and features like "pattern matching for switch"
 * I've struggled. In some cases, light usage of these things was helpful. But in other cases, I got mired in a rigid
 * type hierarchy and can rarely get exhaustive switches to fit.
 */
public class ExecutionContext {

    /**
     * When the matching phase is complete, the root node's matching rows can be taken as the final matching rows.
     */
    public BitSet matchingRows() {
        return rootNode.matchingBits;
    }

    /**
     * A stateful node in the execution context graph.
     * <p>
     * It represents a table, matching indices, and relationships to parent and child nodes.
     * <p>
     * Consider creating a RootNode type which is different because it doesn't have a parent.
     */
    public static class Node {

        private final List<IntPredicate> columnPredicates = new ArrayList<>();
        final Table table;
        private final BitSet matchingBits;

        final Node parent;
        private final AssociationColumn associationToParent;

        public List<Node> childNodes() {
            return List.copyOf(childNodes);
        }

        private final List<Node> childNodes = new ArrayList<>();

        private Node(Table table, Node parent, AssociationColumn associationToParent) {
            this.table = table;
            this.parent = parent;
            this.associationToParent = associationToParent;
            this.matchingBits = new BitSet(table.size());
        }

        public void addColumnPredicate(IntPredicate columnPredicate) {
            columnPredicates.add(columnPredicate);
        }

        public Node createChildNode(AssociationColumn associationToChild) {
            var childNode = new Node(associationToChild.associatedEntity(), this, associationToChild.reverseAssociatedColumn());
            childNodes.add(childNode);
            return childNode;
        }

        /**
         * Filter the child nodes based on the column predicates.
         * <p>
         * Decision: the filter produces a "matching indices" array. It does not create a new table. Note: remember that
         * tables and columns are immutable, so it's not like we can modify those things anyway.
         * <p>
         * The filter method is designed to be called exactly once. There may be multiple passes of "association filters"
         * based on the result of other nodes.
         */
        public void filterSelf() {
            // Combine all the predicates using the convenient "and" method.
            Optional<IntPredicate> combinedPredicateOpt = columnPredicates.stream().reduce(IntPredicate::and);

            if (combinedPredicateOpt.isEmpty()) {
                // When there are no predicates, there is no specific filtering work to do. Technically, all rows match.
                matchingBits.set(0, table.size());
                return;
            }

            var combinedPredicate = combinedPredicateOpt.get();

            for (int i = 0; i < table.size(); i++) {
                if (combinedPredicate.test(i)) matchingBits.set(i);
            }
        }

        /**
         * This is an "upwards" filter. This method narrows that parent node's matching bits to the rows of the
         * parent that are associated from rows in the current node.
         */
        public void filterParent() {
            if (parent == null) return; // The root node is the only node without a parent.

            var parentMatchingBitsByAssociation = new BitSet(parent.table.size());

            for (int i = 0; i < table.size(); i++) {
                if (!matchingBits.get(i)) continue;

                Association upwardsAssociation = associationToParent.associationsForIndex(i);

                switch (upwardsAssociation) {
                    case Association.Many(var indices) -> {
                        for (int index : indices) parentMatchingBitsByAssociation.set(index);
                    }
                    case Association.One(var index) -> parentMatchingBitsByAssociation.set(index);
                    case Association.None ignored -> {
                        // No-op
                    }
                }
            }

            parent.matchingBits.and(parentMatchingBitsByAssociation);
        }
    }

    public final Node rootNode;

    public ExecutionContext(Table rootTable) {
        this.rootNode = new Node(rootTable, null, null);
    }
}
