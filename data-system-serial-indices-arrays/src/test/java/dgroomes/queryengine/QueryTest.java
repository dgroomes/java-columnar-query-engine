package dgroomes.queryengine;

import dgroomes.data_system_serial_indices_arrays.DataSystemSerialIndices;
import dgroomes.datasystem.*;
import dgroomes.datasystem.QueryResult.Failure;
import dgroomes.datasystem.QueryResult.Success;
import dgroomes.inmemory.InMemoryColumn;
import dgroomes.inmemory.InMemoryColumn.StringColumn;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static dgroomes.inmemory.InMemoryColumn.ofInts;
import static dgroomes.inmemory.InMemoryColumn.ofStrings;
import static dgroomes.inmemory.InMemoryTable.ofColumns;
import static dgroomes.queryengine.TestUtil.failed;
import static org.assertj.core.api.Assertions.assertThat;


/**
 * Let's write some test code to help drive our design of the API.... I'm starting from square one here.
 * <p>
 * Note: maybe consider creating a AssertJ extensions to make the test code more palatable. It's quite verbose. But also
 * consider if that's overkill.
 */
public class QueryTest {

    private DataSystemSerialIndices dataSystem;

    @BeforeEach
    void setUp() {
        dataSystem = new DataSystemSerialIndices();
    }

    /**
     * Ordinal integer query over a single-column table.
     */
    @Test
    void intQuery_oneColumnTable() {
        // Arrange
        var table = ofColumns(ofInts(-1, 0, 1, 2, 3));
        dataSystem.register("ints", table);
        var query = new Query("ints");
        query.rootNode.addCriteria(new Criteria.IntCriteria(0, i1 -> i1 > 0));

        // Act
        QueryResult result = dataSystem.execute(query);

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
            case Success(var resultTable) -> resultTable.columns();
        };

        assertThat(columns).hasSize(1);
        Column firstColumn = columns.get(0);

        if (!(firstColumn instanceof InMemoryColumn.IntegerColumn(var ints))) {
            throw failed("Expected an IntegerColumn but got a " + firstColumn.getClass().getSimpleName());
        }

        assertThat(ints).containsExactly(1, 2, 3);
    }

    /**
     * Ordinal integer query over a multi-column table.
     */
    @Test
    void intQuery_twoColumnTable() {
        // Arrange
        var table = ofColumns(
                // City names
                ofStrings("Minneapolis", "Rochester", "Duluth"),

                // City populations
                ofInts(425_336, 121_395, 86_697));
        dataSystem.register("cities", table);
        var query = new Query("cities");
        query.rootNode.addCriteria(new Criteria.IntCriteria(1, pop -> pop > 100_000 && pop < 150_000));

        // Act
        QueryResult result = dataSystem.execute(query);

        // Assert
        var columns = switch (result) {
            case Failure(var msg) -> throw failed(msg);
            case Success(var resultTable) -> resultTable.columns();
        };

        assertThat(columns).hasSize(2);
        Column cityColumn = columns.get(0);

        if (!(cityColumn instanceof InMemoryColumn.StringColumn(var cities))) {
            throw failed("Expected a StringColumn but got a " + cityColumn.getClass().getSimpleName());
        }

        assertThat(cities).containsExactly("Rochester");
    }

    /**
     * Multi-criteria query.
     */
    @Test
    void multiCriteria_rootEntity() {
        // Arrange
        //
        // We're going to search over a simple collection of strings to find those that are greater than "a" but less than
        // "d". This test case is interesting because we're exercising two criteria in a single query.
        var table = ofColumns(ofStrings("a", "a", "b", "c", "c", "d"));
        dataSystem.register("strings", table);

        var query = new Query("strings");
        query.rootNode
                .addCriteria(new Criteria.StringCriteria(0, s -> s.compareTo("a") > 0))
                .addCriteria(new Criteria.StringCriteria(0, s -> s.compareTo("d") < 0));

        // Act
        QueryResult result = dataSystem.execute(query);

        // Assert
        var columns = switch (result) {
            case Failure(var msg) -> throw failed(msg);
            case Success(var resultTable) -> resultTable.columns();
        };

        assertThat(columns).hasSize(1);
        Column firstColumn = columns.get(0);

        if (!(firstColumn instanceof StringColumn(var strings))) {
            throw failed("Expected a StringColumn but got a " + firstColumn.getClass().getSimpleName());
        }

        assertThat(strings).containsExactly("b", "c", "c");
    }

    /**
     * Associations. Given a type X that is associated with another type Y, query for entities of X on a property of Y.
     * Specifically, let's model cities, states and the "contained in" association from city to state.
     */
    @Test
    void queryOnAssociationProperty() {
        var cities = ofColumns(ofStrings("Minneapolis", "Pierre", "Duluth"));
        dataSystem.register("cities", cities);
        var states = ofColumns(ofStrings("Minnesota", "South Dakota"));
        dataSystem.register("states", states);
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
            var query = new Query("cities");
            Query.Node statesNode = query.rootNode.createChild(1);
            statesNode.addCriteria(new Criteria.StringCriteria(0, "South Dakota"::equals));

            // Act
            QueryResult result = dataSystem.execute(query);

            // Assert
            var columns = switch (result) {
                case Failure(var msg) -> throw failed(msg);
                case Success(var resultTable) -> resultTable.columns();
            };

            assertThat(columns).hasSize(2);
            Column cityColumn = columns.get(0);

            if (!(cityColumn instanceof StringColumn(var cityMatches))) {
                throw failed("Expected a StringColumn but got a " + cityColumn.getClass().getSimpleName());
            }

            assertThat(cityMatches).containsExactly("Pierre");
        }

        // Query for Minnesota cities
        {
            var query = new Query("cities");
            Query.Node statesNode = query.rootNode.createChild(1);
            statesNode.addCriteria(new Criteria.StringCriteria(0, "Minnesota"::equals));

            // Act
            QueryResult result = dataSystem.execute(query);

            // Assert
            var columns = switch (result) {
                case Failure(var msg) -> throw failed(msg);
                case Success(var resultTable) -> resultTable.columns();
            };

            assertThat(columns).hasSize(2);
            Column cityColumn = columns.get(0);

            if (!(cityColumn instanceof StringColumn(var cityMatches))) {
                throw failed("Expected a StringColumn but got a " + cityColumn.getClass().getSimpleName());
            }

            assertThat(cityMatches).containsExactly("Minneapolis", "Duluth");
        }
    }

    @Test
    void multiCriteria_includingIntermediateEntity() {
        // Consider a botanical garden. It is full of many sections of plants. There might be a section of cedar trees,
        // a section of maple trees, a section of rose bushes, a section of tulips, etc. There are exactly three
        // botanical categories of plants: trees, shrubs and ferns. Each section is one of those categories.
        //
        // We want to plan a trip to the botanical garden for a friend who has particular taste, and they enjoy variety
        // and efficiency. The friend has all these requirements:
        //
        //   * Variety: visit a section for each of the categories. We need to visit a tree a shrub and a fern.
        //   * Efficiency: subsequent sections must be directly adjacent to previous sections. We don't want to waste
        //     time and effort travelling extra sections.
        //   * Particular: they want to visit trees first because they are strong, then shrubs, then ferns because they
        //     are majestic.
        //
        // For example, consider that there are the following sections:
        //
        //   Section      | Category
        //   -------------+---------
        //   lilacs       | shrubs
        //   cedar trees  | trees
        //   maple trees  | trees
        //   rose bushes  | shrubs
        //   Boston ferns | ferns
        //
        //
        // And the sections are laid out in the garden like this:
        //
        // ┌─────────────┬─────────────┬─────────────┐
        // │ maple trees │    lilacs   │             │
        // │      0      │      1      │      2      │
        // ├─────────────┼─────────────┼─────────────┤
        // │             │             │             │
        // │      3      │      4      │      5      │
        // ├─────────────┼─────────────┼─────────────┤
        // │ Boston fern │  rose bush  │ cedar trees │
        // │      6      │      7      │      8      │
        // └─────────────┴─────────────┴─────────────┘
        //
        // The only path through the garden that satisfies the friend's requirements is to visit the cedar trees, then
        // the rose bush and then the Boston ferns.

        var sections = ofColumns(
                ofStrings(
                        "maple trees", "lilacs", "",
                        "", "", "",
                        "Boston ferns", "rose bush", "cedar trees"),
                ofStrings(
                        "trees", "shrubs", "",
                        "", "", "",
                        "ferns", "shrubs", "trees"));
        dataSystem.register("sections", sections);

        sections.associateTo(sections,
                // The maple trees (0) are adjacent to the lilacs (1) and blank (3)
                Association.toMany(1, 3),

                // The lilacs (1) are adjacent to the maple trees (0) and blank (2) and blank (4)
                Association.toMany(0, 2, 4),

                // blank (2) is adjacent to the lilacs (1) and blank (5)
                Association.toMany(1, 5),

                // blank (3) is adjacent to the maple trees (0), blank (4) and the Boston ferns (6)
                Association.toMany(0, 4, 6),

                // blank (4) is adjacent to the lilacs (1), blank (3), blank (5) and the rose bush (7)
                Association.toMany(1, 3, 5, 7),

                // blank (5) is adjacent to blank (2), blank (4) and the cedar trees (8)
                Association.toMany(2, 4, 8),

                // The Boston ferns (6) are adjacent to blank (3) and the rose bush (7)
                Association.toMany(3, 7),

                // The rose bush (7) is adjacent to blank (4), the Boston ferns (6) and the cedar trees (8)
                Association.toMany(4, 6, 8),

                // The cedar trees (8) are adjacent to blank (5) and the rose bush (7)
                Association.toMany(5, 7));

        // Let's write the query. NOTE: it's not possible to express the "shrubs" adjacent to "ferns" in the API now. I
        // need to figure out how to implement that.

        var query = new Query("sections");
        query.rootNode.addCriteria(new Criteria.StringCriteria(1, "trees"::equals))
                .createChild(2)
                .addCriteria(new Criteria.StringCriteria(1, "shrubs"::equals))
                .createChild(2)
                .addCriteria(new Criteria.StringCriteria(1, "ferns"::equals));

        // Act
        QueryResult result = dataSystem.execute(query);

        // Assert
        var columns = switch (result) {
            case Failure(var msg) -> throw failed(msg);
            case Success(var resultTable) -> resultTable.columns();
        };

        // The first column is the name (e.g. "cedar trees")
        // The second column is the category (e.g. "trees")
        // The third column is the association column to itself
        // The fourth column is the reverse association column
        assertThat(columns).hasSize(4);
        Column nameColumn = columns.get(0);

        if (!(nameColumn instanceof StringColumn(var nameMatches))) {
            throw failed("Expected a StringColumn but got a " + nameColumn.getClass().getSimpleName());
        }

        assertThat(nameMatches).containsExactly("cedar trees");
    }
}
