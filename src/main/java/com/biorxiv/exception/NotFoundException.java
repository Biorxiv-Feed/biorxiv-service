package com.biorxiv.exception;

/**
 * Thrown when an entity that the caller expects to exist is missing.
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String entityName, Object id) {
        super(entityName + " not found: " + id);
    }
}
