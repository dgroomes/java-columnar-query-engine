package dgroomes.queryengine;

import dgroomes.queryengine.ObjectGraph.Column.IntegerColumn;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.stream.IntStream;

/**
 * WIP
 * <p>
 * This is a pattern matching query engine (like the 'cypher' query language). It does not do aggregations.
 * There is no query "language" and instead it's just a Java API.
 */
public class Executor {

  // columns? entities? joins/associations? schemas?

  /**
   * WIP
   * <p>
   * Given a query (should the query encapsulate the data source? answer: probably not the data source but probably the
   * schema?), return a list of matching records.
   * <p>
   * Tentative decision: this does not return a cursor.
   * <p>
   * Tentative decision: this returns a pruned object graph of values. For direct arrays, this is simple. Given an array
   * of integers, return an array of the matching integers. For multi-column graphs, consider an object graph that
   * represents a city. It has two columns: "city_name" and "state code". Return a new multi-column graph where its
   * component columns are pruned down to the entries that matched the criteria. (this is a squishy concept for me..).
   * <p>
   * Tentative decision: it's ok to not parse criteria like "x < 5" and instead just use lambda. In this way, we can't
   * do a whole category of optimization but I don't care to do that.
   * <p>
   * Decision: full scans are good. I don't care about indexing. This data is in-memory and we want to support criteria
   * like regex which can't take advantage of indexes anyway.
   * <p>
   * I don't care much about generics here. I just want to get something working.
   */
  public static QueryResult match(Query query, ObjectGraph objectGraph) {
    if (query instanceof Query.SingleFieldIntegerQuery query1) {
      if (objectGraph instanceof IntegerColumn boxed) {
        int[] matches = Arrays.stream(boxed.ints())
                .filter(query1::match)
                .toArray();
        return new QueryResult.Success(matches);
      }

      var msg = "The object graph type '%s' cannot be queried by query type '%s'".formatted(objectGraph.getClass().getSimpleName(), query.getClass().getSimpleName());
      return new QueryResult.Failure(msg);
    } else if (query instanceof Query.OrdinalSingleFieldIntegerQuery query1) {
      if (objectGraph instanceof ObjectGraph.MultiColumnEntity multiColumnEntity) {

        if (multiColumnEntity.columns().size() < query1.ordinal()) {
          var msg = "The query ordinal '%d' is out of bounds for the object graph with %d columns".formatted(query1.ordinal(), multiColumnEntity.columns().size());
          return new QueryResult.Failure(msg);
        }

        ObjectGraph.Column column1 = multiColumnEntity.columns().get(query1.ordinal());

        // Make sure the types match. For example, a query directed at column 3 must match an object graph with an integer
        // column in the 3rd position.
        if (column1 instanceof IntegerColumn intColumn) {
          int[] indexMatches = IntStream.range(0, intColumn.ints().length)
                  .filter(i -> query1.intCriteria().match(intColumn.ints()[i]))
                  .toArray();

          ObjectGraph.MultiColumnEntity pruned = multiColumnEntity.prune(indexMatches);
          return new QueryResult.Success(pruned);
        } else {
          return new QueryResult.Failure("The queried column type '%s' is not an integer array but the query is for an integer.".formatted(column1.getClass().getSimpleName()));
        }
      } else {
        var msg = "The object graph type '%s' cannot be queried by query type '%s'".formatted(objectGraph.getClass().getSimpleName(), query.getClass().getSimpleName());
        return new QueryResult.Failure(msg);
      }
    } else if (query instanceof Query.PointerSingleFieldStringQuery(Query.Pointer pointer, Query.StringCriteria stringCriteria)) {
      // todo implement a recursive/iterative search algorithm. Here is some hard work.
      //
      // Algorithm working notes. We need to descend the object graph following the pointer, all the while verifying that
      // the pointer is "legal" (meaning it follows columns that exist and all the columns are association columns except
      // for the last one which is a string column). When we reach the end of the pointer, we can apply the string criteria.
      // But then we have to follow back up the pointer and prune the object graph.

      if (objectGraph instanceof ObjectGraph.MultiColumnEntity multiColumnEntity) {
        // I need to group things together during the query execution, like grouping columns with their owning entities.
        // This is some unfortunate glue code. consider refactoring.
        //
        // We group an entity and an associated column together. Because an associated column points to another entity,
        // we call this a "directional context". Hopefully this helps me understand what I'm doing.
        record DirectionalContextGrouping(ObjectGraph.MultiColumnEntity entity, ObjectGraph.Column.AssociationColumn associationColumn) {}

        Deque<DirectionalContextGrouping> groupings = new ArrayDeque<>();

        ObjectGraph.MultiColumnEntity currentEntity = multiColumnEntity;
        Query.Pointer currentPointer = pointer;
        int[] indexMatches;
        while (true) {
          if (currentPointer instanceof Query.Pointer.Ordinal(int ordinal)) {
            // We've bottomed out. Match the data on the criteria.
            if (currentEntity.columns().size() < ordinal) {
              var msg = "The query ordinal '%d' is out of bounds for the object graph with %d columns".formatted(ordinal, currentEntity.columns().size());
              return new QueryResult.Failure(msg);
            }

            ObjectGraph.Column column = currentEntity.columns().get(ordinal);
            if (column instanceof ObjectGraph.Column.StringColumn stringColumn) {
              indexMatches = IntStream.range(0, stringColumn.strings().length)
                      .filter(i -> stringCriteria.match(stringColumn.strings()[i]))
                      .toArray();
              break;
            } else {
              return new QueryResult.Failure("The queried column type '%s' is not a string array but the query is for a string.".formatted(column.getClass().getSimpleName()));
            }

          } else if (currentPointer instanceof Query.Pointer.NestedPointer(int ordinal, Query.Pointer nextPointer)) {
            var associationColumn = (ObjectGraph.Column.AssociationColumn) currentEntity.columns().get(ordinal);
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
          ObjectGraph.Column.AssociationColumn associationColumn = group.associationColumn();
          ObjectGraph.Association[] reverseAssociations = associationColumn.reverseAssociatedColumn().associations;
          var nextMatches = new HashSet<Integer>();
          for (int i : indexMatches) {
            ObjectGraph.Association reverseAssociation = reverseAssociations[i];
            var toAdd = switch (reverseAssociation) {
              case ObjectGraph.Association.Many(var indices) -> indices;
              case ObjectGraph.Association.One(var index) -> new int[]{index};
              case ObjectGraph.Association.None ignored -> EMPTY;
            };
            for (int toAddI : toAdd) {
              nextMatches.add(toAddI);
            }
          }
          indexMatches = nextMatches.stream().mapToInt(i -> i).toArray();
        }
      } else {
        var msg = "The object graph type '%s' cannot be queried by query type '%s'".formatted(objectGraph.getClass().getSimpleName(), query.getClass().getSimpleName());
        return new QueryResult.Failure(msg);
      }
    } else {
      var msg = "This query type is not yet implemented: %s".formatted(query.getClass().getSimpleName());
      return new QueryResult.Failure(msg);
    }
  }

  public sealed interface QueryResult permits QueryResult.Success, QueryResult.Failure {
    record Success(Object matches) implements QueryResult {
    }

    record Failure(String message) implements QueryResult {
    }
  }
}
