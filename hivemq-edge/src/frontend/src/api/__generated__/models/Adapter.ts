/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

import type { Status } from './Status';

export type Adapter = {
    /**
     * The adapter configuration associated with this instance
     */
    config?: Record<string, Record<string, any>>;
    /**
     * The adapter id, must be unique and only contain alpha numeric characters with spaces and hyphens.
     */
    id: string;
    status?: Status;
    /**
     * The adapter type associated with this instance
     */
    type?: string;
};

