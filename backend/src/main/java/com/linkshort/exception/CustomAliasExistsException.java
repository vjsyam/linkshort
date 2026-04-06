package com.linkshort.exception;

/**
 * Thrown when a user-chosen custom alias already exists in the database.
 */
public class CustomAliasExistsException extends RuntimeException {
    public CustomAliasExistsException(String alias) {
        super("Custom alias already taken: " + alias);
    }
}
