/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

import type { AdapterRuntimeInformation } from './AdapterRuntimeInformation';

export type Adapter = {
    adapterRuntimeInformation?: AdapterRuntimeInformation;
    /**
     * The adapter configuration associated with this instance
     */
    config?: Record<string, Record<string, any>>;
    /**
     * The adapter id, must be unique and only contain alpha numeric characters with spaces and hyphens.
     */
    id: string;
    /**
     * The adapter type associated with this instance
     */
    type?: string;
};

