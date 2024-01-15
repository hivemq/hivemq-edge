/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

import type { PaginationCursor } from './PaginationCursor';
import type { Script } from './Script';

/**
 * A listing of scripts.
 */
export type ScriptList = {
    _links?: PaginationCursor;
    /**
     * List of result items that are returned by this endpoint
     */
    items?: Array<Script>;
};

