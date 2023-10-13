package com.hivemq.edge.model;

/**
 * @author Simon L Johnson
 */
public interface Identifiable {

    /**
     * Represents a uniquely identifiable object in the system.
     * @return The system-wide identifier of the object
     */
    TypeIdentifier getIdentifier();
}
