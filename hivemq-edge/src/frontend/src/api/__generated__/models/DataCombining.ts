/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

import type { Instruction } from './Instruction';

/**
 * Define individual rules for data combining, based on the entities selected in the Orchestrator
 */
export type DataCombining = {
    /**
     * The unique id of the data combining mapping
     */
    id: string;
    sources?: {
        primary: string;
        primaryType: DataCombining.primaryType;
        /**
         * The list of tags (names) used in the data combining
         */
        tags?: Array<string>;
        /**
         * The list of topic filters (names) used in the data combining
         */
        topicFilters?: Array<string>;
    };
    destination: {
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

export namespace DataCombining {

    export enum primaryType {
        TAG = 'TAG',
        TOPIC_FILTER = 'TOPIC_FILTER',
    }


}

