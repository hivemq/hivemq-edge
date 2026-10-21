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
    /**
     * Whether messages on this mapping are method calls. When enabled, each message must name the method to call in 'methodName', and that method is called on the tag's object. Required for a method tag: with it switched off no method is called whatever the message says, and a request to a method tag is refused with Bad_MethodInvalid naming this setting.
     */
    methodCalls?: boolean;
    /**
     * Whether requests carry a correlation token in 'correlationData', which is read and echoed unchanged on the response. A field that is not enabled is discarded rather than passed on.
     */
    correlationData?: boolean;
    /**
     * Whether requests may name their own response topic in 'responseTopic'. This is the only setting involved: with it enabled a request's response goes to the address the request named, instead of the northbound mapping's own topic. A field that is not enabled is discarded rather than passed on.
     */
    responseTopic?: boolean;
};

