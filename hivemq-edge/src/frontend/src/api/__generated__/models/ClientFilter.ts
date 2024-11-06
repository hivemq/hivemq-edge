/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

import type { ClientFilterConfiguration } from './ClientFilterConfiguration';

/**
 * A client filter
 */
export type ClientFilter = {
    /**
     * The unique id of the client filter
     */
    id: string;
    /**
     * The list of topics associated with this client filter
     */
    topicFilters: Array<ClientFilterConfiguration>;
};

