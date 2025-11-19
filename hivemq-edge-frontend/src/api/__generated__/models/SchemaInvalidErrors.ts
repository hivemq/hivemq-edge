/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

import type { ApiProblemDetails } from './ApiProblemDetails';
import type { SchemaValidationError } from './SchemaValidationError';

export type SchemaInvalidErrors = (ApiProblemDetails & {
    /**
     * List of child validation errors.
     */
    childErrors: Array<SchemaValidationError>;
});

