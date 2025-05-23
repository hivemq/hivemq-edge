/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

import type { Link } from './Link';

/**
 * List of result items that are returned by this endpoint
 */
export type Module = {
    /**
     * The module author
     */
    author?: string;
    /**
     * The module description
     */
    description?: string | null;
    documentationLink?: Link;
    /**
     * A mandatory ID associated with the Module
     */
    id?: string;
    /**
     * Is the module installed
     */
    installed?: boolean | null;
    logoUrl?: Link;
    /**
     * The type of the module
     */
    moduleType?: string | null;
    /**
     * The module name
     */
    name?: string;
    /**
     * The module priority
     */
    priority?: number;
    provisioningLink?: Link;
    /**
     * The module version
     */
    version?: string;
};

