/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

import type { DomainTag } from './DomainTag';

export type DomainTagOwnerList = {
    /**
     * List of result items that are returned by this endpoint
     */
    items: Array<{
        /**
         * The id of the adapter owning the tag
         */
        adapterId: string;
        mapping: DomainTag;
    }>;
};

