package dgroomes.queryengine;

import dgroomes.queryapi.Criteria;
import dgroomes.queryapi.Pointer;
import dgroomes.queryapi.Query;
import dgroomes.queryengine.Column.IntegerColumn;
import dgroomes.queryengine.Executor.QueryResult;
import dgroomes.queryengine.Executor.QueryResult.Failure;
import dgroomes.queryengine.Executor.QueryResult.Success;
import org.junit.jupiter.api.Test;

import java.util.List;

import static dgroomes.queryengine.Column.ofInts;
import static dgroomes.queryengine.Table.ofColumns;
import static dgroomes.queryengine.TestUtil.failed;
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
   * Ordinal integer query over a single-column table.
   */
  @Test
  void intQuery_oneColumnTable() {
    // Arrange
    var table = ofColumns(ofInts(-1, 0, 1, 2, 3));
    var query = new Query.OrdinalSingleFieldIntegerQuery(0, i -> i > 0);

    // Act
    QueryResult result = Executor.match(query, table);

    // Assert
    var columns = switch (result) {
      // By using the 'throw' keyword here, we are being good partners with the compiler because the compiler lets us
      // narrow the type of `result` to `Success` and better yet, we can even extract a reference to the underlying list
      // of columns because of the "record pattern matching" language features.
      //
      // If, by contrast, we used AssertJ to verify the type like this:
      //
      //     assertThat(result).isInstanceOf(Success.class);
      //
      // We would get a semantic assertion, but the compiler doesn't care. We would have to cast 'result' to a 'Success'.
      // That's not so bad, but it is redundant. This style isn't necessarily better, but I want to keep exercising
      // my Java language skills.
      case Failure(var msg) -> throw failed(msg);
      case Success(Table(List<Column> c)) -> c;
    };

    assertThat(columns).hasSize(1);
    Column firstColumn = columns.get(0);

    if (!(firstColumn instanceof IntegerColumn(var ints))) {
      throw failed("Expected an IntegerColumn but got a " + firstColumn.getClass().getSimpleName());
    }

    assertThat(ints).containsExactly(1, 2, 3);
  }

  /**
   * [Happy Path]
   * Ordinal integer query over a multi-column table.
   */
  @Test
  void intQuery_twoColumnTable() {
    // Arrange
    var table = ofColumns(
            // City names
            Column.ofStrings("Minneapolis", "Rochester", "Duluth"),

            // City populations
            ofInts(425_336, 121_395, 86_697));
    var query = new Query.OrdinalSingleFieldIntegerQuery(1, pop -> pop > 100_000 && pop < 150_000);

    // Act
    QueryResult result = Executor.match(query, table);

    // Assert
    var columns = switch (result) {
      case Failure(var msg) -> throw failed(msg);
      case Success(Table(List<Column> c)) -> c;
    };

    assertThat(columns).hasSize(2);
    Column cityColumn = columns.get(0);

    if (!(cityColumn instanceof Column.StringColumn(var cities))) {
      throw failed("Expected a StringColumn but got a " + cityColumn.getClass().getSimpleName());
    }

    assertThat(cities).containsExactly("Rochester");
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
    var table = ofColumns(Column.ofStrings("a", "a", "b", "c", "c", "d"));

    var query = new Query.PointedStringCriteriaQuery(List.of(
            new Criteria.PointedStringCriteria(new Pointer.Ordinal(0), s -> s.compareTo("a") > 0),
            new Criteria.PointedStringCriteria(new Pointer.Ordinal(0), s -> s.compareTo("d") < 0)));

    // Act
    QueryResult result = Executor.match(query, table);

    // Assert
    var columns = switch (result) {
      case Failure(var msg) -> throw failed(msg);
      case Success(Table(List<Column> c)) -> c;
    };

    assertThat(columns).hasSize(1);
    Column firstColumn = columns.get(0);

    if (!(firstColumn instanceof Column.StringColumn(var strings))) {
      throw failed("Expected a StringColumn but got a " + firstColumn.getClass().getSimpleName());
    }

    assertThat(strings).containsExactly("b", "c", "c");
  }

  /**
   * [Happy Path]
   * <p>
   * Associations. Given a type X that is associated with another type Y, query for entities of X on a property of Y.
   * Specifically, let's model cities, states and the "contained in" association from city to state.
   */
  @Test
  void queryOnAssociationProperty() {
    var cities = ofColumns(Column.ofStrings("Minneapolis", "Pierre", "Duluth"));
    var states = ofColumns(Column.ofStrings("Minnesota", "South Dakota"));
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
      var query = new Query.PointedStringCriteriaQuery(List.of(new Criteria.PointedStringCriteria(new Pointer.NestedPointer(1, new Pointer.Ordinal(0)), "South Dakota"::equals)));

      // Act
      QueryResult result = Executor.match(query, cities);

      // Assert
      var columns = switch (result) {
        case Failure(var msg) -> throw failed(msg);
        case Success(Table(List<Column> c)) -> c;
      };

      assertThat(columns).hasSize(2);
      Column cityColumn = columns.get(0);

      if (!(cityColumn instanceof Column.StringColumn(var cityMatches))) {
        throw failed("Expected a StringColumn but got a " + cityColumn.getClass().getSimpleName());
      }

      assertThat(cityMatches).containsExactly("Pierre");
    }

    // Query for Minnesota cities
    {
      var query = new Query.PointedStringCriteriaQuery(List.of(new Criteria.PointedStringCriteria(new Pointer.NestedPointer(1, new Pointer.Ordinal(0)), "Minnesota"::equals)));

      // Act
      QueryResult result = Executor.match(query, cities);

      // Assert
      var columns = switch (result) {
        case Failure(var msg) -> throw failed(msg);
        case Success(Table(List<Column> c)) -> c;
      };

      assertThat(columns).hasSize(2);
      Column cityColumn = columns.get(0);

      if (!(cityColumn instanceof Column.StringColumn(var cityMatches))) {
        throw failed("Expected a StringColumn but got a " + cityColumn.getClass().getSimpleName());
      }

      assertThat(cityMatches).containsExactly("Minneapolis", "Duluth");
    }
  }
}
