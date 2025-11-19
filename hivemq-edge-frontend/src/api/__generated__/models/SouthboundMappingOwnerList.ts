/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

import type { SouthboundMapping } from './SouthboundMapping';

export type SouthboundMappingOwnerList = {
    /**
     * List of result items that are returned by this endpoint
     */
    items: Array<{
        /**
         * The id of the adapter owning the mapping
         */
        adapterId: string;
        mapping: SouthboundMapping;
    }>;
};

