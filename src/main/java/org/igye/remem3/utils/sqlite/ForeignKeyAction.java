package org.igye.remem3.utils.sqlite;

public enum ForeignKeyAction {
    /**
     * Configuring "NO ACTION" means just that: when a parent key is modified or deleted from the database, no
     * special action is taken.
     */
    NO_ACTION("NO ACTION"),
    /**
     * The "RESTRICT" action means that the application is prohibited from deleting (for ON DELETE RESTRICT) or
     * modifying (for ON UPDATE RESTRICT) a parent key when there exists one or more child keys mapped to it. The
     * difference between the effect of a RESTRICT action and normal foreign key constraint enforcement is that the
     * RESTRICT action processing happens as soon as the field is updated - not at the end of the current statement
     * as it would with an immediate constraint, or at the end of the current transaction as it would with a deferred
     * constraint. Even if the foreign key constraint it is attached to is deferred, configuring a RESTRICT action
     * causes SQLite to return an error immediately if a parent key with dependent child keys is deleted or modified.
     */
    RESTRICT("RESTRICT"),
    /**
     * If the configured action is "SET NULL", then when a parent key is deleted (for ON DELETE SET NULL) or modified
     * (for ON UPDATE SET NULL), the child key columns of all rows in the child table that mapped to the parent key
     * are set to contain SQL NULL values.
     */
    SET_NULL("SET NULL"),
    /**
     * The "SET DEFAULT" actions are similar to "SET NULL", except that each of the child key columns is set to
     * contain the column's default value instead of NULL.
     */
    SET_DEFAULT("SET DEFAULT"),
    /**
     * A "CASCADE" action propagates the delete or update operation on the parent key to each dependent child key.
     * For an "ON DELETE CASCADE" action, this means that each row in the child table that was associated with the
     * deleted parent row is also deleted. For an "ON UPDATE CASCADE" action, it means that the values stored in each
     * dependent child key are modified to match the new parent key values.
     */
    CASCADE("CASCADE");

    private final String name;

    ForeignKeyAction(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
