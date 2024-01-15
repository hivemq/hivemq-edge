/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

/**
 * The restrictions that are in effect for this client
 */
export type ClientRestrictions = {
    /**
     * maximum MQTT message size
     */
    maxMessageSize?: number | null;
    /**
     * maximum queue size
     */
    maxQueueSize?: number | null;
    /**
     * The queue strategy if the queue is full
     */
    queuedMessageStrategy?: string | null;
};

