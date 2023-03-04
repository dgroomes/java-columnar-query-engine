package dgroomes.queryengine;

import dgroomes.queryengine.Column.IntegerColumn;
import dgroomes.queryengine.Executor.QueryResult;
import dgroomes.queryengine.Executor.QueryResult.Success;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Let's write some test code to help drive our design of the API.... I'm starting from square one here.
 * <p>
 * Note: maybe consider creating a AssertJ extensions to make the test code more palatable. It's quite verbose. But also
 * consider if that's overkill.
 */
public class QueryEngineTest {

  /**
   * [Happy Path]
   * Ordinal integer query over a multi-field (i.e. column) type that contains one column.
   */
  @Test
  void intQuery_oneColumnTable() {
    Table corpus = Table.ofColumns(Column.ofInts(-1, 0, 1, 2, 3));
    var query = new Query.OrdinalSingleFieldIntegerQuery(0, i -> i > 0);

    // Act
    QueryResult result = Executor.match(query, corpus);

    // Assert
    assertThat(result).isInstanceOf(Success.class);
    Success success = (Success) result;
    Table matchingSubset = success.matchingSubset();
    assertThat(matchingSubset.columns()).hasSize(1);
    Column firstColumn = matchingSubset.columns().get(0);
    assertThat(firstColumn).isInstanceOf(IntegerColumn.class);
    var intMatches = (IntegerColumn) firstColumn;
    assertThat(intMatches.ints()).containsExactly(1, 2, 3);
  }

  /**
   * [Happy Path]
   * Ordinal integer query over a multi-field (i.e. column) type that contains two columns.
   */
  @Test
  void intQuery_twoColumnTable() {
    Table corpus = Table.ofColumns(
            // City names
            Column.ofStrings("Minneapolis", "Rochester", "Duluth"),

            // City populations
            Column.ofInts(425_336, 121_395, 86_697));
    var query = new Query.OrdinalSingleFieldIntegerQuery(1, pop -> pop > 100_000 && pop < 150_000);

    // Act
    QueryResult result = Executor.match(query, corpus);

    // Assert
    assertThat(result).isInstanceOf(Success.class);
    Success success = (Success) result;
    Table matchingSubset = success.matchingSubset();
    assertThat(matchingSubset.columns()).hasSize(2);
    Column cityColumn = matchingSubset.columns().get(0);
    assertThat(cityColumn).isInstanceOf(Column.StringColumn.class);
    var cityMatches = (Column.StringColumn) cityColumn;
    assertThat(cityMatches.strings()).containsExactly("Rochester");
  }

  /**
   * [Happy Path]
   * Multi-criteria query.
   */
  @Test
  void multiCriteria() {
    // Arrange
    //
    // We're going to search over a simple collection of strings to find those that are greater than "a" but less than
    // "d". This test case is interesting because we're exercising two criteria in a single query.
    Table corpus = Table.ofColumns(Column.ofStrings("a", "a", "b", "c", "c", "d"));

    var query = new Query.PointedStringCriteriaQuery(List.of(
            new Query.PointedStringCriteria(new Query.Pointer.Ordinal(0), s -> s.compareTo("a") > 0),
            new Query.PointedStringCriteria(new Query.Pointer.Ordinal(0), s -> s.compareTo("d") < 0)));

    // Act
    QueryResult result = Executor.match(query, corpus);

    // Assert
    assertThat(result).isInstanceOf(Success.class);
    Success success = (Success) result;
    Table matchingSubset = success.matchingSubset();
    assertThat(matchingSubset.columns()).hasSize(1);
    Column firstColumn = matchingSubset.columns().get(0);
    assertThat(firstColumn).isInstanceOf(Column.StringColumn.class);
    var stringMatches = (Column.StringColumn) firstColumn;
    assertThat(stringMatches.strings()).containsExactly("b", "c", "c");
  }

  /**
   * [Happy Path]
   * <p>
   * Associations. Given a type X that is associated with another type Y, query for entities of X on a property of Y.
   * Specifically, let's model cities, states and the "contained in" association from city to state.
   */
  @Test
  void queryOnAssociationProperty() {
    Table cities = Table.ofColumns(Column.ofStrings("Minneapolis", "Pierre", "Duluth"));
    Table states = Table.ofColumns(Column.ofStrings("Minnesota", "South Dakota"));
    // The "contained in" association from city to state. It is based on the index position of the cities and states
    // expressed above.
    cities.associateTo(states,
            Association.toOne(0) /* Minneapolis is contained in Minnesota */,
            Association.toOne(1) /* Pierre is contained in South Dakota */,
            Association.toOne(0) /* Duluth is contained in Minnesota */);
    // Note: '1' is the ordinal position of the "contained in" association column to the state collection.
    // '0' is the ordinal position of the state name column in the state collection.

    // Query for South Dakota cities
    {
      var query = new Query.PointerSingleFieldStringQuery(new Query.Pointer.NestedPointer(1, new Query.Pointer.Ordinal(0)), "South Dakota"::equals);

      // Act
      QueryResult result = Executor.match(query, cities);

      // Assert
      assertThat(result).isInstanceOf(Success.class);
      Success success = (Success) result;
      Table matchingSubset = success.matchingSubset();
      assertThat(matchingSubset.columns()).hasSize(2);
      Column cityColumn = matchingSubset.columns().get(0);
      assertThat(cityColumn).isInstanceOf(Column.StringColumn.class);
      var cityMatches = (Column.StringColumn) cityColumn;
      assertThat(cityMatches.strings()).containsExactly("Pierre");
    }

    // Query for Minnesota cities
    {
      var query = new Query.PointerSingleFieldStringQuery(new Query.Pointer.NestedPointer(1, new Query.Pointer.Ordinal(0)), "Minnesota"::equals);

      // Act
      QueryResult result = Executor.match(query, cities);

      // Assert
      assertThat(result).isInstanceOf(Success.class);
      Success success = (Success) result;
      Table matchingSubset = success.matchingSubset();
      assertThat(matchingSubset.columns()).hasSize(2);
      Column cityColumn = matchingSubset.columns().get(0);
      assertThat(cityColumn).isInstanceOf(Column.StringColumn.class);
      var cityMatches = (Column.StringColumn) cityColumn;
      assertThat(cityMatches.strings()).containsExactly("Minneapolis", "Duluth");
    }
  }
}
