/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

import type { DataCombiningList } from './DataCombiningList';
import type { EntityReferenceList } from './EntityReferenceList';

/**
 * A data combiner, bringing tags (adapters) and topic filters (bridges) together for further northbound data mapping
 */
export type Combiner = {
    /**
     * The unique id of the data combiner
     */
    id: string;
    /**
     * The user-facing name of the combiner
     */
    name: string;
    /**
     * The user-facing description of the combiner
     */
    description?: string;
    sources?: EntityReferenceList;
    mappings?: DataCombiningList;
};

