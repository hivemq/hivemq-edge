/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

import type { JsonNode } from './JsonNode';

/**
 * List of result items that are returned by this endpoint
 */
export type ProtocolAdapter = {
    /**
     * The author of the adapter
     */
    author?: string;
    configSchema?: JsonNode;
    /**
     * The description
     */
    description?: string;
    id?: string;
    /**
     * The logo of the adapter
     */
    logoUrl?: string;
    /**
     * The name of the adapter
     */
    name?: string;
    /**
     * The supported protocol
     */
    protocol?: string;
    /**
     * The url of the adapter
     */
    url?: string;
    /**
     * The installed version of the adapter
     */
    version?: string;
};

