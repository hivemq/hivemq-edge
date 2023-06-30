/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

/**
 * List of result items that are returned by this endpoint
 */
export type Listener = {
    /**
     * The extension description
     */
    description?: string | null;
    /**
     * A mandatory ID hostName with the Listener
     */
    hostName?: string;
    /**
     * The listener name
     */
    name?: string;
    /**
     * The extension port
     */
    port?: number;
};

