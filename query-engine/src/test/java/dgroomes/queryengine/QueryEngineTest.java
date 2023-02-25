package dgroomes.queryengine;

import dgroomes.queryengine.Executor.QueryResult;
import dgroomes.queryengine.Executor.QueryResult.Success;
import dgroomes.queryengine.ObjectGraph.Column.IntegerColumn;
import dgroomes.queryengine.ObjectGraph.MultiColumnEntity;
import dgroomes.queryengine.Query.SingleFieldIntegerQuery;
import org.junit.jupiter.api.Test;

import static dgroomes.queryengine.ObjectGraph.of;
import static dgroomes.queryengine.ObjectGraph.ofInts;
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
   * Single-field integer query over a direct array.
   * <p>
   */
  @Test
  void intQuery_intArray() {
    // Arrange
    //
    // Let's write a simple query over simple data.
    //
    // Data-under-test. This type is like a single-column table.
    IntegerColumn corpus = ofInts(-1, 0, 1, 2, 3);
    // Let's search for positive (non-zero) numbers.
    SingleFieldIntegerQuery query = integerUnderTest -> integerUnderTest > 0;

    // Act
    QueryResult result = Executor.match(query, corpus);

    // Assert
    assertThat(result).isInstanceOf(Success.class);
    Success success = (Success) result;
    Object matches = success.matches();
    assertThat(matches).isInstanceOf(int[].class);
    int[] intMatches = (int[]) matches;
    assertThat(intMatches).containsExactly(1, 2, 3);
  }

  /**
   * [Happy Path]
   * Single-field integer query over a multi-field (i.e. column) type.
   */
  @Test
  void intQuery_multiFieldType() {
    // Arrange
    //
    // Let's write a simple query over simple data.
    //
    // Data-under-test. This type is like a single-column table.
    MultiColumnEntity corpus = of(ofInts(-1, 0, 1, 2, 3));
    // Let's search for positive (non-zero) numbers.
    var query = new Query.OrdinalSingleFieldIntegerQuery(0, integerUnderTest -> integerUnderTest > 0);

    // Act
    QueryResult result = Executor.match(query, corpus);

    // Assert
    assertThat(result).isInstanceOf(Success.class);
    Success success = (Success) result;
    Object matches = success.matches();
    assertThat(matches).isInstanceOf(MultiColumnEntity.class);
    MultiColumnEntity multiColumnEntityMatches = (MultiColumnEntity) matches;
    assertThat(multiColumnEntityMatches.columns()).hasSize(1);
    ObjectGraph.Column firstColumn = multiColumnEntityMatches.columns().get(0);
    assertThat(firstColumn).isInstanceOf(IntegerColumn.class);
    IntegerColumn intMatches = (IntegerColumn) firstColumn;
    assertThat(intMatches.ints()).containsExactly(1, 2, 3);
  }
}
