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
     * TODO[28249] Changed manually until backend fixed
     * A user created description for this tag.
     */
    definition: JsonNode;
    /**
     * TODO[28249] Changed manually until backend fixed
     * The name of the tag that identifies it within this edge instance.
     */
    name: string;
};

