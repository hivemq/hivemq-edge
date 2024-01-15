/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

import type { PaginationCursor } from './PaginationCursor';
import type { Schema } from './Schema';

/**
 * A listing of schemas.
 */
export type SchemaList = {
    _links?: PaginationCursor;
    /**
     * List of result items that are returned by this endpoint
     */
    items?: Array<Schema>;
};

