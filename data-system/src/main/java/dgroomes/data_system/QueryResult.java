package dgroomes.data_system;

sealed public interface QueryResult permits QueryResult.Success, QueryResult.Failure {
    record Success(Table resultSet) implements QueryResult {
    }

    record Failure(String message) implements QueryResult {
    }
}
