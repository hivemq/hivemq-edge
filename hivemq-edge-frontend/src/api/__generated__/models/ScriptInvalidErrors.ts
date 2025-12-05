/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

import type { ApiProblemDetails } from './ApiProblemDetails';
import type { ScriptValidationError } from './ScriptValidationError';

export type ScriptInvalidErrors = (ApiProblemDetails & {
    /**
     * List of child validation errors.
     */
    childErrors: Array<ScriptValidationError>;
});

