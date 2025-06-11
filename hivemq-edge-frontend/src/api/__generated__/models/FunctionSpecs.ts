/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

import type { FunctionMetadata } from './FunctionMetadata';
import type { JsonNode } from './JsonNode';

/**
 * The configuration of a DataHub operation function
 */
export type FunctionSpecs = {
    /**
     * The unique name of the function
     */
    functionId: string;
    /**
     * The metadata associated with the function
     */
    metadata: FunctionMetadata;
    /**
     * the full JSON-Schema describimng the function and its arguments
     */
    schema: JsonNode;
    /**
     * An optional UI Schema to customise the rendering of the configuraton form
     */
    uiSchema?: JsonNode;
};

