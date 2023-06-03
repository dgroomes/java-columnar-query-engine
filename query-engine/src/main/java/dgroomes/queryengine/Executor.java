package dgroomes.queryengine;

import dgroomes.queryapi.Criteria;
import dgroomes.queryapi.Pointer;
import dgroomes.queryengine.Column.IntegerColumn;
import dgroomes.util.Util;

import java.util.*;
import java.util.function.IntPredicate;
import java.util.stream.IntStream;

/**
 * This is a toy pattern matching query engine (like the 'cypher' query language). It does not do aggregations.
 * There is no query "language" and instead it's just a Java API.
 */
public class Executor {

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
    public static QueryResult match(List<? extends Criteria> criteriaList, Table table) {
        Objects.requireNonNull(criteriaList, "The 'criteriaList' argument must not be null");
        Objects.requireNonNull(table, "The 'table' argument must not be null");

        // Our strategy is to start with the assumption that all entries in the entity match and prune down this list
        // with each criterion.
        int[] rootIndexMatches = IntStream.range(0, table.size()).toArray();

        criteriaLoop:
        for (Criteria criterion : criteriaList) {

            // Algorithm working notes. We need to descend the table's fields and its associated tables following the pointer, all the while verifying that
            // the pointer is "legal" (meaning it follows columns that exist and all the columns are association columns except
            // for the last one which is a scalar column). When we reach the end of the pointer, we can apply the scalar criterion.
            // After that, we have to follow back up the pointer and prune the table.
            var pointer = criterion.pointer();

            // I need to group things together during the query execution, like grouping columns with their owning entities.
            // This is some unfortunate glue code. Consider refactoring.
            //
            // We group an entity and an associated column together. Because an associated column points to another entity,
            // we call this a "directional context". Hopefully this helps me understand what I'm doing.
            record DirectionalContextGrouping(Table entity,
                                              Column.AssociationColumn associationColumn) {}

            Deque<DirectionalContextGrouping> groupings = new ArrayDeque<>();

            Table currentEntity = table;
            Pointer currentPointer = pointer;
            int[] indexMatches;
            while (true) {
                if (currentPointer instanceof Pointer.Ordinal(int ordinal)) {
                    // We've bottomed out. Match the data on the scalar (integer, string, boolean) criterion.
                    if (currentEntity.columns().size() < ordinal) {
                        var msg = "The query ordinal '%d' is out of bounds for the table with %d columns".formatted(ordinal, currentEntity.columns().size());
                        return new QueryResult.Failure(msg);
                    }

                    Column column = currentEntity.columns().get(ordinal);

                    IntPredicate indexPredicate = switch (column) {
                        case Column.StringColumn stringColumn -> {
                            if (!(criterion instanceof Criteria.PointedStringCriteria pointedStringCriteria))
                                throw new IllegalArgumentException("The column is a string column but the criterion is not a string predicate. This is unexpected.");

                            yield i -> pointedStringCriteria.stringPredicate().test(stringColumn.strings()[i]);
                        }
                        case Column.IntegerColumn intColumn -> {
                            if (!(criterion instanceof Criteria.PointedIntCriteria pointedIntCriteria))
                                throw new IllegalArgumentException("The column is an integer column but the criterion is not an integer predicate. This is unexpected.");

                            yield i -> pointedIntCriteria.integerPredicate().test(intColumn.ints()[i]);
                        }
                        case Column.BooleanColumn ignored ->
                                throw new IllegalStateException("Boolean columns are not supported yet.");
                        case Column.AssociationColumn ignored ->
                                throw new IllegalStateException("Association columns can't be matched on with a scalar criteria. That doesn't make sense.");
                    };

                    indexMatches = IntStream.range(0, column.height())
                            .filter(indexPredicate)
                            .toArray();
                    break;
                } else if (currentPointer instanceof Pointer.NestedPointer(int ordinal, Pointer nextPointer)) {
                    var associationColumn = (Column.AssociationColumn) currentEntity.columns().get(ordinal);
                    groupings.push(new DirectionalContextGrouping(currentEntity, associationColumn));
                    currentPointer = nextPointer;
                    currentEntity = associationColumn.associatedEntity;
                } else {
                    return new QueryResult.Failure("I'm not sure what happened.");
                }
            }

            int[] EMPTY = new int[0];

            // Follow the matched indices back up the graph by tracing the associations.
            while (true) {
                if (groupings.isEmpty()) {
                    // Prune down the root index matches to the ones that match this pointed criteria search.
                    // We don't use the actual 'prune' method but instead do an intersection of the two integer arrays. Because
                    // the arrays are ordered, we can do a zipper intersection.
                    rootIndexMatches = Util.zipperIntersection(rootIndexMatches, indexMatches);
                    // Yes I'm using a named 'continue'! Probably a very unpopular choice but during the exploratory work, it's
                    // convenient to write top-down code and so we don't have the luxury of the return statement because that
                    // would return from the entire method.
                    continue criteriaLoop;
                }

                DirectionalContextGrouping grouping = groupings.pop();
                Column.AssociationColumn associationColumn = grouping.associationColumn();
                Association[] reverseAssociations = associationColumn.reverseAssociatedColumn().associations;
                var nextMatches = new HashSet<Integer>();
                for (int i : indexMatches) {
                    Association reverseAssociation = reverseAssociations[i];
                    var toAdd = switch (reverseAssociation) {
                        case Association.Many(var indices) -> indices;
                        case Association.One(var index) -> new int[]{index};
                        case Association.None ignored -> EMPTY;
                    };
                    for (int toAddI : toAdd) {
                        nextMatches.add(toAddI);
                    }
                }
                indexMatches = nextMatches.stream().mapToInt(i -> i).toArray();
            }
        }
        return toResultSet(table, rootIndexMatches);
    }

    /**
     * Convenience method for matching a single criterion.
     */
    public static QueryResult match(Criteria criterion, Table table) {
        return match(List.of(criterion), table);
    }

    /**
     * Prune the table down to the rows at the matching indices This represents the final "result set" of the query.
     * <p>
     * This is designed to be used after evaluating the query criteria where we have identified a set of rows (by index)
     * that matched the criteria.
     */
    private static QueryResult.Success toResultSet(Table table, int[] indices) {
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
