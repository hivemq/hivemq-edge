/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

import type { NorthboundMapping } from './NorthboundMapping';

export type NorthboundMappingOwnerList = {
    /**
     * List of result items that are returned by this endpoint
     */
    items: Array<{
        /**
         * The id of the adapter owning the mapping
         */
        adapterId: string;
        mapping: NorthboundMapping;
    }>;
};

