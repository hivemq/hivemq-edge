/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

import type { DataPolicy } from './DataPolicy';
import type { PaginationCursor } from './PaginationCursor';

/**
 * A listing of data policies.
 */
export type DataPolicyList = {
    _links?: PaginationCursor;
    /**
     * List of result items that are returned by this endpoint
     */
    items?: Array<DataPolicy>;
};

