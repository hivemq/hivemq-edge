/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

import type { Link } from './Link';

/**
 * List of result items that are returned by this endpoint
 */
export type Extension = {
    /**
     * The extension author
     */
    author?: string;
    /**
     * The extension description
     */
    description?: string | null;
    /**
     * A mandatory ID associated with the Extension
     */
    id?: string;
    /**
     * Is the extension installed
     */
    installed?: boolean | null;
    link?: Link;
    /**
     * The extension name
     */
    name?: string;
    /**
     * The extension priority
     */
    priority?: number;
    /**
     * The extension version
     */
    version?: string;
};

