/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

import type { ClientSubscription } from './ClientSubscription';
import type { PaginationCursor } from './PaginationCursor';

export type ClientSubscriptionList = {
    _links?: PaginationCursor;
    /**
     * List of result items that are returned by this endpoint
     */
    items?: Array<ClientSubscription>;
};

