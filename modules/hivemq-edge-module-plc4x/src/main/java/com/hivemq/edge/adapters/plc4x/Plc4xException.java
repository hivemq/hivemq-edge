package com.hivemq.edge.adapters.plc4x;

/**
 * @author Simon L Johnson
 */
public class Plc4xException extends Exception {
    public Plc4xException() {
    }

    public Plc4xException(final String message) {
        super(message);
    }

    public Plc4xException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public Plc4xException(final Throwable cause) {
        super(cause);
    }
}
