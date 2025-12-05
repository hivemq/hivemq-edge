/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

import type { ApiProblemDetails } from './ApiProblemDetails';
import type { BehaviorPolicyValidationError } from './BehaviorPolicyValidationError';

export type BehaviorPolicyInvalidErrors = (ApiProblemDetails & {
    /**
     * List of child validation errors.
     */
    childErrors: Array<BehaviorPolicyValidationError>;
});

