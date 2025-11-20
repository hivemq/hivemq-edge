/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

import type { ApiProblemDetails } from './ApiProblemDetails';
import type { DataPolicyValidationError } from './DataPolicyValidationError';

export type DataPolicyInvalidErrors = (ApiProblemDetails & {
    /**
     * List of child validation errors.
     */
    childErrors: Array<DataPolicyValidationError>;
});

