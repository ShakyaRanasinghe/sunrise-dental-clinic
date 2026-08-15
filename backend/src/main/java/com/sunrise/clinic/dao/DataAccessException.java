package com.sunrise.clinic.dao;

/**
 * Wraps a {@link java.sql.SQLException} as an unchecked exception.
 *
 * <p>The service layer is written against the repository interfaces and should not
 * have to declare or handle a checked exception that is purely an artefact of the
 * storage technology. Wrapping it here keeps JDBC from leaking upwards while
 * preserving the original exception as the cause for the logs.</p>
 */
public class DataAccessException extends RuntimeException {

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
