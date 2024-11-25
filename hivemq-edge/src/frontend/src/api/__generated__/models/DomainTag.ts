/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

import type { JsonNode } from './JsonNode';

/**
 * List of result items that are returned by this endpoint
 */
export type DomainTag = {
    /**
     * A user created description for this tag.
     */
    description?: string;
    /**
     * The protocol id of the protocol for which this tag was created.
     */
    protocolId: string;
    /**
     * A user created description for this tag.
     */
    tagDefinition: JsonNode;
    /**
     * The name of the tag that identifies it within this edge instance.
     */
    tagName: string;
};

