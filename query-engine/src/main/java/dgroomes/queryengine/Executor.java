package dgroomes.queryengine;

import dgroomes.queryapi.Query;
import dgroomes.queryengine.Column.IntegerColumn;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
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
   * Tentative decision: this does not return a cursor.
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
  public static QueryResult match(Query query, Table table) {
    if (query instanceof Query.OrdinalSingleFieldIntegerQuery query1) {
      if (table instanceof Table multiColumnEntity) {

        if (multiColumnEntity.columns().size() < query1.ordinal()) {
          var msg = "The query ordinal '%d' is out of bounds for the table with %d columns".formatted(query1.ordinal(), multiColumnEntity.columns().size());
          return new QueryResult.Failure(msg);
        }

        Column column1 = multiColumnEntity.columns().get(query1.ordinal());

        // Make sure the types match. For example, a query directed at column 3 must match a table with an integer
        // column in the 3rd position.
        if (column1 instanceof IntegerColumn intColumn) {
          int[] indexMatches = IntStream.range(0, intColumn.ints().length)
                  .filter(i -> query1.intCriteria().match(intColumn.ints()[i]))
                  .toArray();

          Table pruned = multiColumnEntity.prune(indexMatches);
          return new QueryResult.Success(pruned);
        } else {
          return new QueryResult.Failure("The queried column type '%s' is not an integer array but the query is for an integer.".formatted(column1.getClass().getSimpleName()));
        }
      } else {
        var msg = "The table type '%s' cannot be queried by query type '%s'".formatted(table.getClass().getSimpleName(), query.getClass().getSimpleName());
        return new QueryResult.Failure(msg);
      }
    } else if (query instanceof Query.PointerSingleFieldStringQuery(
            Query.Pointer pointer, Query.StringCriteria stringCriteria
    )) {
      // Implement a recursive/iterative search algorithm. Here is some hard work.
      //
      // Algorithm working notes. We need to descend the table's fields and its associated tables following the pointer, all the while verifying that
      // the pointer is "legal" (meaning it follows columns that exist and all the columns are association columns except
      // for the last one which is a string column). When we reach the end of the pointer, we can apply the string criteria.
      // But then we have to follow back up the pointer and prune the table.

      if (table instanceof Table multiColumnEntity) {
        // I need to group things together during the query execution, like grouping columns with their owning entities.
        // This is some unfortunate glue code. consider refactoring.
        //
        // We group an entity and an associated column together. Because an associated column points to another entity,
        // we call this a "directional context". Hopefully this helps me understand what I'm doing.
        record DirectionalContextGrouping(Table entity,
                                          Column.AssociationColumn associationColumn) {}

        Deque<DirectionalContextGrouping> groupings = new ArrayDeque<>();

        Table currentEntity = multiColumnEntity;
        Query.Pointer currentPointer = pointer;
        int[] indexMatches;
        while (true) {
          if (currentPointer instanceof Query.Pointer.Ordinal(int ordinal)) {
            // We've bottomed out. Match the data on the criteria.
            if (currentEntity.columns().size() < ordinal) {
              var msg = "The query ordinal '%d' is out of bounds for the table with %d columns".formatted(ordinal, currentEntity.columns().size());
              return new QueryResult.Failure(msg);
            }

            Column column = currentEntity.columns().get(ordinal);
            if (column instanceof Column.StringColumn stringColumn) {
              indexMatches = IntStream.range(0, stringColumn.strings().length)
                      .filter(i -> stringCriteria.match(stringColumn.strings()[i]))
                      .toArray();
              break;
            } else {
              return new QueryResult.Failure("The queried column type '%s' is not a string array but the query is for a string.".formatted(column.getClass().getSimpleName()));
            }

          } else if (currentPointer instanceof Query.Pointer.NestedPointer(int ordinal, Query.Pointer nextPointer)) {
            var associationColumn = (Column.AssociationColumn) currentEntity.columns().get(ordinal);
            groupings.add(new DirectionalContextGrouping(currentEntity, associationColumn));
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
            var pruned = currentEntity.prune(indexMatches);
            return new QueryResult.Success(pruned);
          }

          DirectionalContextGrouping group = groupings.pop();
          currentEntity = group.entity();
          Column.AssociationColumn associationColumn = group.associationColumn();
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
      } else {
        var msg = "The table type '%s' cannot be queried by query type '%s'".formatted(table.getClass().getSimpleName(), query.getClass().getSimpleName());
        return new QueryResult.Failure(msg);
      }
    } else if (query instanceof Query.PointedStringCriteriaQuery pointedCriteriaQuery) {
      // Most of this is copy/pasted from the earlier block. I'm going to refactor this eventually because I'm still in
      // discovery mode.
      if (table instanceof Table multiColumnEntity) {
        // Our strategy is to start with the assumption that all entries in the entity match and prune down this list
        // with each criteria.
        int[] rootIndexMatches = IntStream.range(0, multiColumnEntity.size()).toArray();

        criteriaLoop:
        for (Query.PointedStringCriteria pointedStringCriteria : pointedCriteriaQuery.pointedCriteriaList()) {
          var pointer = pointedStringCriteria.pointer();
          var stringCriteria = pointedStringCriteria.criteria();

          // I need to group things together during the query execution, like grouping columns with their owning entities.
          // This is some unfortunate glue code. consider refactoring.
          //
          // We group an entity and an associated column together. Because an associated column points to another entity,
          // we call this a "directional context". Hopefully this helps me understand what I'm doing.
          record DirectionalContextGrouping(Table entity,
                                            Column.AssociationColumn associationColumn) {}

          Deque<DirectionalContextGrouping> groupings = new ArrayDeque<>();

          Table currentEntity = multiColumnEntity;
          Query.Pointer currentPointer = pointer;
          int[] indexMatches;
          while (true) {
            if (currentPointer instanceof Query.Pointer.Ordinal(int ordinal)) {
              // We've bottomed out. Match the data on the criteria.
              if (currentEntity.columns().size() < ordinal) {
                var msg = "The query ordinal '%d' is out of bounds for the table with %d columns".formatted(ordinal, currentEntity.columns().size());
                return new QueryResult.Failure(msg);
              }

              Column column = currentEntity.columns().get(ordinal);
              if (column instanceof Column.StringColumn stringColumn) {
                indexMatches = IntStream.range(0, stringColumn.strings().length)
                        .filter(i -> stringCriteria.match(stringColumn.strings()[i]))
                        .toArray();
                break;
              } else {
                return new QueryResult.Failure("The queried column type '%s' is not a string array but the query is for a string.".formatted(column.getClass().getSimpleName()));
              }

            } else if (currentPointer instanceof Query.Pointer.NestedPointer(int ordinal, Query.Pointer nextPointer)) {
              var associationColumn = (Column.AssociationColumn) currentEntity.columns().get(ordinal);
              groupings.add(new DirectionalContextGrouping(currentEntity, associationColumn));
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
              int[] pruned = new int[indexMatches.length]; // This size is a bit arbitrary. I just made it the max size we I don't have to bother resizing it during the zipper procedure.
              int prunedIndex = 0;
              int rootIndexMatchesIndex = 0;
              int indexMatchesIndex = 0;
              while (rootIndexMatchesIndex < rootIndexMatches.length && indexMatchesIndex < indexMatches.length) {
                if (rootIndexMatches[rootIndexMatchesIndex] == indexMatches[indexMatchesIndex]) {
                  pruned[prunedIndex++] = rootIndexMatches[rootIndexMatchesIndex];
                  rootIndexMatchesIndex++;
                  indexMatchesIndex++;
                } else if (rootIndexMatches[rootIndexMatchesIndex] < indexMatches[indexMatchesIndex]) {
                  rootIndexMatchesIndex++;
                } else {
                  indexMatchesIndex++;
                }
              }

              rootIndexMatches = Arrays.copyOf(pruned, prunedIndex);
              // Yes I'm using a 'continue'! Probably a very unpopular choice but during the exploratory work, it's
              // convenient to write top-down code and so we don't have the luxury of the return statement because that
              // would return from the entire method.
              continue criteriaLoop;
            }

            DirectionalContextGrouping group = groupings.pop();
            // Is this a problem that it's never used? Do I need 'currentEntity'?
            //            currentEntity = group.entity();
            Column.AssociationColumn associationColumn = group.associationColumn();
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

        var pruned = multiColumnEntity.prune(rootIndexMatches);
        return new QueryResult.Success(pruned);
      } else {
        var msg = "The table type '%s' cannot be queried by query type '%s'".formatted(table.getClass().getSimpleName(), query.getClass().getSimpleName());
        return new QueryResult.Failure(msg);
      }
    } else {
      var msg = "This query type is not yet implemented: %s".formatted(query.getClass().getSimpleName());
      return new QueryResult.Failure(msg);
    }
  }

  public sealed interface QueryResult permits QueryResult.Success, QueryResult.Failure {
    record Success(Table matchingSubset) implements QueryResult {
    }

    record Failure(String message) implements QueryResult {
    }
  }
}
