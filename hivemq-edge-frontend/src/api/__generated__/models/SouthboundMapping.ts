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
     * The tag for which values hould be collected and sent out.
     */
    tagName: string;
    /**
     * The filter defining what topics we will receive messages from.
     */
    topicFilter: string;
};

