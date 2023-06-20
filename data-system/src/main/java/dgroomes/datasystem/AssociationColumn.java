package dgroomes.datasystem;

public interface AssociationColumn extends Column {

    Table associatedEntity();

    AssociationColumn reverseAssociatedColumn();

    /**
     * For the row at the given index, return the associations from that row to the rows in the associated (other)
     * table.
     */
    Association associationsForIndex(int i);
}
