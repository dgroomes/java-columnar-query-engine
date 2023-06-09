package dgroomes.queryengine;

import dgroomes.queryapi.Criteria;
import dgroomes.queryapi.Pointer;

import java.util.List;
import java.util.Objects;

/**
 * A query verifier (or at least, my naive guess at what a query verifier is. Is this a linker? A compiler?).
 * <p>
 * We want to take a {@link dgroomes.queryapi.Criteria} and create an "execution context" from it. This is a
 * graph that is a physical representation of the query execution process. It incorporates tables as nodes.
 */
public class Verifier {

    public sealed interface VerificationResult {

        record LegalQuery(ExecutionContext executionContext) implements VerificationResult {}

        record IllegalQuery(String message) implements VerificationResult {}
    }

    /**
     * Verify if the given {@link Criteria} is legal for the given {@link Table}. If the criteria describes columns that
     * don't exist or types that don't match, then the query is illegal.
     *
     * @param criteriaList the criteria to verify
     * @param table The table to verify the criteria against
     * @return a {@link VerificationResult} that indicates if the query is legal or not. For legal queries, the result
     * will contain an {@link ExecutionContext} that can be used to execute the query.
     */
    public VerificationResult verify(List<? extends Criteria> criteriaList, Table table) {
        Objects.requireNonNull(criteriaList, "The 'criteriaList' argument must not be null");
        Objects.requireNonNull(table, "The 'table' argument must not be null");

        var executionContext = new ExecutionContext(table);

        for (Criteria criterion : criteriaList) {

            // Algorithm working notes. We need to descend the table's fields and its associated tables following the pointer, all the while verifying that
            // the pointer is "legal" (meaning it follows columns that exist and all the columns are association columns except
            // for the last one which is a scalar column)
            var pointer = criterion.pointer();

            ExecutionContext.Node currentNode = executionContext.rootNode;
            Pointer currentPointer = pointer;

            while (true) {
                if (currentPointer instanceof Pointer.Ordinal(int ordinal)) {
                    // We've bottomed out. This node is a leaf node. (Note: I don't think this is a good design but this is how it is for now.)
                    if (currentNode.table.columns().size() < ordinal) {
                        var msg = "The query ordinal '%d' is out of bounds for the table with %d columns".formatted(ordinal, currentNode.table.columns().size());
                        return new VerificationResult.IllegalQuery(msg);
                    }

                    Column column = currentNode.table.columns().get(ordinal);

                    switch (column) {
                        case Column.StringColumn stringColumn -> {
                            if (!(criterion instanceof Criteria.PointedStringCriteria pointedStringCriteria))
                                return new VerificationResult.IllegalQuery("The column is a string column but the criterion is not a string predicate. This is unexpected.");
                            currentNode.addColumnPredicate(idx -> pointedStringCriteria.stringPredicate().test(stringColumn.strings()[idx]));
                        }
                        case Column.IntegerColumn intColumn -> {
                            if (!(criterion instanceof Criteria.PointedIntCriteria pointedIntCriteria))
                                throw new IllegalArgumentException("The column is an integer column but the criterion is not an integer predicate. This is unexpected.");
                            currentNode.addColumnPredicate(i -> pointedIntCriteria.integerPredicate().test(intColumn.ints()[i]));
                        }
                        case Column.BooleanColumn ignored -> {
                            return new VerificationResult.IllegalQuery("Boolean columns are not supported yet.");
                        }
                        case Column.AssociationColumn ignored -> {
                            return new VerificationResult.IllegalQuery("Association columns can't be matched on with a scalar criteria. That doesn't make sense.");
                        }
                        case default -> throw new IllegalStateException("Unrecognized column type: %s".formatted(column.getClass().getName()));
                    }
                    break;
                } else if (currentPointer instanceof Pointer.NestedPointer(int ordinal, Pointer nextPointer)) {
                    var associationColumn = (Column.AssociationColumn) currentNode.table.columns().get(ordinal);
                    var childNode = currentNode.createChildNode(associationColumn);
                    currentPointer = nextPointer;
                    currentNode = childNode;
                } else {
                    return new VerificationResult.IllegalQuery("Found a Pointer type that is not supported: %s".formatted(currentPointer.getClass().getName()));
                }
            }
        }

        return new VerificationResult.LegalQuery(executionContext);
    }
}
