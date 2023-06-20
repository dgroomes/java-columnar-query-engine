package dgroomes.datasystem;

/**
 * An instance of {@link DataSystem} is a "read-only object datastore and query engine". It is a specific data system
 * limited to the goals of this project. Basically, it lets you query table-like data with a primitive query API.
 * <p>
 * Specific implementations of {@link DataSystem} offer a way to load data into the system but that varies for different
 * providers. For a SQL-based system, for example, the write/load interface would be JDBC.
 */
public interface DataSystem {

    /**
     * Execute the given query and return a list of matching records.
     * <p>
     * The result set is a pruned table. Consider a table that represents a city. It has two columns: "city_name" and
     * "state code". Return a new table where its component columns are pruned down to the rows that
     * matched the criteria.
     */
    QueryResult match(Query query, Table table);
}
