/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

import type { BehaviorPolicy } from './BehaviorPolicy';
import type { PaginationCursor } from './PaginationCursor';

/**
 * A listing of behavior policies.
 */
export type BehaviorPolicyList = {
    _links?: PaginationCursor;
    /**
     * List of result items that are returned by this endpoint
     */
    items?: Array<BehaviorPolicy>;
};

