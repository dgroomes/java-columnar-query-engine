package dgroomes.inmemory;

import dgroomes.datamodel.Association;
import dgroomes.datamodel.Column;
import dgroomes.datamodel.Table;

import java.util.*;
import java.util.stream.IntStream;

/**
 * A {@link Table} implemented with simple in-memory data structures.
 */
public class InMemoryTable implements Table {

    private final List<InMemoryColumn> columns;

    public InMemoryTable(List<InMemoryColumn> columns) {
        this.columns = columns;
    }

    /**
     * Note: consider making the column list unmodifiable for callers.
     */
    @Override
    public List<? extends Column> columns() {
        return columns;
    }

    /**
     * Convenience method for creating an {@link InMemoryColumn.IntegerColumn} for toy examples, like in test cases.
     */
    public static InMemoryTable ofColumns(InMemoryColumn... columns) {
        var mutableList = new ArrayList<>(Arrays.asList(columns));
        return new InMemoryTable(mutableList);
    }

    /**
     * Associate this entity (X) to another entity (Y).
     *
     * @param associatedEntity the entity type to associate to (Y)
     * @param associations     the associations from this entity (X) to the associated entity (Y)
     * @return the association column
     */
    public InMemoryColumn.AssociationColumn associateTo(InMemoryTable associatedEntity, Association... associations) {
        var associationColumn = new InMemoryColumn.AssociationColumn(associatedEntity, associations);
        // Note: yes this is nasty; we are using a mutable data structure. I love using records, but I often wind up with
        // a need to mutate it and struggle.
        this.columns.add(associationColumn);

        // Create a reverse association from Y to X.
        //
        // Note: this is some stream of consciousness code and needs to be cleaned up.
        InMemoryColumn.AssociationColumn reverseAssociationColumn;
        {
            var yIndexToXAssociations = new HashMap<Integer, List<Integer>>();
            // Initialize the map with empty lists.
            for (int yIndex = 0; yIndex < associatedEntity.size(); yIndex++) {
                yIndexToXAssociations.put(yIndex, new ArrayList<>());
            }

            for (int xIndex = 0; xIndex < associations.length; xIndex++) {
                int[] yIndices = switch (associations[xIndex]) {
                    case Association.One(var idx) -> new int[]{idx};
                    case Association.Many(var indices) -> indices;
                    case Association.None ignored -> new int[]{};
                    case null -> throw new IllegalStateException("Found a null association");
                };

                for (int yIndex : yIndices) {
                    var yToX = yIndexToXAssociations.get(yIndex);
                    yToX.add(xIndex);
                }
            }

            Association[] yToXAssociations = IntStream.range(0, associatedEntity.size()).mapToObj(yIndex -> {
                List<Integer> xIndices = yIndexToXAssociations.get(yIndex);
                return switch (xIndices.size()) {
                    case 0 -> Association.None.NONE;
                    case 1 -> new Association.One(xIndices.get(0));
                    default -> new Association.Many(xIndices.stream().mapToInt(i -> i).toArray());
                };
            }).toArray(Association[]::new);
            reverseAssociationColumn = new InMemoryColumn.AssociationColumn(this, yToXAssociations);
            reverseAssociationColumn.setReverseAssociatedColumn(associationColumn);
            associationColumn.setReverseAssociatedColumn(reverseAssociationColumn);
        }

        associatedEntity.columns.add(reverseAssociationColumn);
        return associationColumn;
    }

    public int size() {
        // This implementation is silly.
        var column = columns.get(0);
        return switch (column) {
            case InMemoryColumn.BooleanColumn boolColumn -> boolColumn.bools().length;
            case InMemoryColumn.IntegerColumn intColumn -> intColumn.ints().length;
            case InMemoryColumn.StringColumn stringColumn -> stringColumn.strings().length;
            case InMemoryColumn.AssociationColumn associationColumn -> associationColumn.associations.length;
        };
    }

    /**
     * This method is designed to create the result set of the query. A subset of the original table is the result set.
     */
    @Override
    public Table subset(BitSet matchingRows) {
        var prunedColumns = columns.stream()
                .<InMemoryColumn>map(column -> switch (column) {
                    case InMemoryColumn.BooleanColumn(var bools) -> {
                        var pruned = new boolean[matchingRows.cardinality()];
                        int j = 0;
                        for (int i = 0; i < size(); i++) {
                            if (!matchingRows.get(i)) continue;

                            pruned[j] = bools[i];
                            j++;
                        }
                        yield new InMemoryColumn.BooleanColumn(pruned);
                    }
                    case InMemoryColumn.IntegerColumn(var ints) -> {
                        var pruned = new int[matchingRows.cardinality()];
                        int j = 0;
                        for (int i = 0; i < size(); i++) {
                            if (!matchingRows.get(i)) continue;

                            pruned[j] = ints[i];
                            j++;
                        }
                        yield new InMemoryColumn.IntegerColumn(pruned);
                    }
                    case InMemoryColumn.StringColumn(var strings) -> {
                        var pruned = new String[matchingRows.cardinality()];
                        int j = 0;
                        for (int i = 0; i < size(); i++) {
                            if (!matchingRows.get(i)) continue;

                            pruned[j] = strings[i];
                            j++;
                        }
                        yield new InMemoryColumn.StringColumn(pruned);
                    }
                    case InMemoryColumn.AssociationColumn associationColumn -> {
                        var associations = associationColumn.associations;
                        var pruned = new Association[matchingRows.cardinality()];
                        int j = 0;
                        for (int i = 0; i < size(); i++) {
                            if (!matchingRows.get(i)) continue;

                            pruned[j] = associations[i];
                            j++;
                        }
                        yield new InMemoryColumn.AssociationColumn(associationColumn.associatedEntity, pruned);
                    }
                })
                .toList();

        return new InMemoryTable(prunedColumns);
    }
}
