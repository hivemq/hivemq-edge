/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

import type { DataIdentifierReference } from './DataIdentifierReference';
import type { Instruction } from './Instruction';

/**
 * Define individual rules for data combining, based on the entities selected in the Orchestrator
 */
export type DataCombining = {
    /**
     * The unique id of the data combining mapping
     */
    id: string;
    sources: {
        primary: DataIdentifierReference;
        /**
         * The list of tags (names) used in the data combining
         * @deprecated This field will be removed in a future API version. Ownership information is tracked via formContext.selectedSources in the frontend. Reconstructed from instructions on load.
         */
        tags?: Array<string>;
        /**
         * The list of topic filters (names) used in the data combining
         * @deprecated This field will be removed in a future API version. Ownership information is tracked via formContext.selectedSources in the frontend. Reconstructed from instructions on load.
         */
        topicFilters?: Array<string>;
    };
    destination: {
        /**
         * The id of a mapped asset containing the topic and schema used for destination of the mapping
         */
        assetId?: string;
        /**
         * The MQTT topic used for destination of the mapping
         */
        topic?: string;
        /**
         * The optional json schema for this topic filter in the data uri format.
         */
        schema?: string;
    };
    /**
     * List of instructions to be applied to incoming data
     */
    instructions: Array<Instruction>;
};

