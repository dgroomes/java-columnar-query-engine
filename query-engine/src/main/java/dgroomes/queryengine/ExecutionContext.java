package dgroomes.queryengine;

import dgroomes.util.Util;

import java.util.*;
import java.util.function.IntPredicate;
import java.util.stream.IntStream;

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
     * When the matching phase is complete, the root node's indices can be taken as the final matching indices.
     */
    public int[] matchingIndices() {
        return rootNode.matchingIndices;
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
        public final Table table;
        private int[] matchingIndices;
        public final Node parent;
        public final Column.AssociationColumn reverseAssociationToParent;

        public List<Node> childNodes() {
            return List.copyOf(childNodes);
        }

        private final List<Node> childNodes = new ArrayList<>();

        public Node(Table table, Node parent, Column.AssociationColumn reverseAssociationToParent) {
            this.table = table;
            this.parent = parent;
            this.reverseAssociationToParent = reverseAssociationToParent;

            // We start with the assumption that all values in the column match. This is indeed a wasteful allocation
            // but this is the model we have for now.
            matchingIndices = IntStream.range(0, table.size()).toArray();
        }

        public void addColumnPredicate(IntPredicate columnPredicate) {
            columnPredicates.add(columnPredicate);
        }

        public Node createChildNode(Column.AssociationColumn associationColumn) {
            var childNode = new Node(associationColumn.associatedEntity, this, associationColumn.reverseAssociatedColumn());
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
        public void filter() {
            Optional<IntPredicate> combinedPredicateOpt = columnPredicates.stream().reduce(IntPredicate::and);
            if (combinedPredicateOpt.isEmpty()) return;

            var combinedPredicate = combinedPredicateOpt.get();
            matchingIndices = Arrays.stream(matchingIndices)
                    .filter(combinedPredicate)
                    .toArray();
        }

        public void filterParent() {
            if (parent == null) return; // The root node is the only node without a parent.

            // Because the arrays are ordered, we can do a zipper intersection.
            Association[] reverseAssociations = reverseAssociationToParent.associations;
            var associationMatches = new HashSet<Integer>();

            var EMPTY = new int[0];
            for (int i : matchingIndices) {
                Association reverseAssociation = reverseAssociations[i];
                var toAdd = switch (reverseAssociation) {
                    case Association.Many(var indices) -> indices;
                    case Association.One(var index) -> new int[]{index};
                    case Association.None ignored -> EMPTY;
                };
                for (int toAddI : toAdd) {
                    associationMatches.add(toAddI);
                }
            }

            // I didn't really want to pay for the sorting but not sure what else to do about right now.
            var associationMatchesArr = associationMatches.stream().mapToInt(i -> i).sorted().toArray();

            parent.matchingIndices = Util.zipperIntersection(parent.matchingIndices, associationMatchesArr);
        }
    }

    public final Node rootNode;

    public ExecutionContext(Table rootTable) {
        this.rootNode = new Node(rootTable, null, null);
    }
}
