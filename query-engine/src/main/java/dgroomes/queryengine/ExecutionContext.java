package dgroomes.queryengine;

import dgroomes.datamodel.Association;
import dgroomes.datamodel.AssociationColumn;
import dgroomes.datamodel.Table;
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
        final Table table;
        private int[] matchingIndices;
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

            // We start with the assumption that all values in the column match. This is indeed a wasteful allocation
            // but this is the model we have for now.
            matchingIndices = IntStream.range(0, table.size()).toArray();
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
        public void filter() {
            Optional<IntPredicate> combinedPredicateOpt = columnPredicates.stream().reduce(IntPredicate::and);
            if (combinedPredicateOpt.isEmpty()) return;

            var combinedPredicate = combinedPredicateOpt.get();
            matchingIndices = Arrays.stream(matchingIndices)
                    .filter(combinedPredicate)
                    .toArray();
        }

        /**
         * This is an "upwards" filter. This method narrows that parent node's matching indices to the rows of the
         * parent that are associated from rows in the current node.
         */
        public void filterParent() {
            if (parent == null) return; // The root node is the only node without a parent.

            // This represents indices *of the parent* that are pointed to by *live rows* of the current node.
            var associationMatches = new HashSet<Integer>();

            var EMPTY = new int[0];
            for (int i : matchingIndices) {
                Association reverseAssociation = associationToParent.associationsForIndex(i);
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

            // Because the arrays are ordered, we can do a zipper intersection.
            parent.matchingIndices = Util.zipperIntersection(parent.matchingIndices, associationMatchesArr);
        }
    }

    public final Node rootNode;

    public ExecutionContext(Table rootTable) {
        this.rootNode = new Node(rootTable, null, null);
    }
}
