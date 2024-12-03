/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

import type { FieldMapping } from './FieldMapping';

/**
 * List of result items that are returned by this endpoint
 */
export type SouthboundMapping = {
    fieldMapping?: FieldMapping;
    /**
     * The maximum MQTT-QoS for the outgoing messages.
     */
    maxQoS: number;
    /**
     * TODO[28498] Changed manually until backend fixed
     * The tag for which values hould be collected and sent out.
     */
    tagName?: string;
    /**
     * TODO[28498] Changed manually until backend fixed
     * The filter defining what topics we will receive messages from.
     */
    topicFilter?: string;
};

