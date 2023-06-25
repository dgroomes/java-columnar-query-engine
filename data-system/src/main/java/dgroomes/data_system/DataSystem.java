package dgroomes.data_system;

/**
 * An instance of {@link DataSystem} is a "read-only object datastore and query engine".
 * <p>
 * It is a specific data system limited to the goals of this project. Basically, it lets you query table-like data with
 * a primitive pattern matching query API (like the 'cypher' query language). It does not do aggregations. There is no
 * query "language" but instead just a Java API for expressing queries.
 * <p>
 * Consider it a toy implementation of parts of a database. It does not do aggregations.
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
     *
     * Note: should the result set instead be a List of Maps? Or even a list of Objects or even an Object?
     * Now that I've decided the physical data encapsulated by the data system could be anything, then doesn't it make
     * sense that the return value is anything? Now we've reached the area where the "data projection" part of the query
     * is desirable (e.g. the "select a, b, c" part of the query; we've only specified criteria e.g. the "where ...").
     * Maybe the compromise is to return a List<Map<String, Object>> (where Object is value types only). Indeed that is
     * like a traditional flat result set from a SQL query.
     */
    QueryResult execute(Query query);
}
