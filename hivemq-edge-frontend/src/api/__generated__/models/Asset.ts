/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

/**
 * The definition of an asset as sourced from the Pulse Broker
 */
export type Asset = {
    /**
     * The unique id of the asset
     */
    readonly id: string;
    /**
     * The user-facing name of the asset
     */
    readonly name: string;
    /**
     * The topic associated with the asset
     */
    readonly topic: string;
    /**
     * The schema associated with the asset, in a JSON Schema and data uri format.
     */
    schema: string;
};

