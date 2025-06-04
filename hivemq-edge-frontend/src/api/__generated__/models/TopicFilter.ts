/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

/**
 * List of result items that are returned by this endpoint
 */
export type TopicFilter = {
    /**
     * The name for this topic filter.
     */
    description?: string;
    /**
     * The optional json schema for this topic filter in the data uri format.
     */
    schema?: string;
    /**
     * The topic filter according to the MQTT specification.
     */
    topicFilter: string;
};

