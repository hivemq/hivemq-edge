/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

import type { ValidationError } from './ValidationError';

export type InvalidSchemaVersionValidationError = (ValidationError & {
    /**
     * The schema id.
     */
    id: string;
    /**
     * The schema version.
     */
    version: string;
});

