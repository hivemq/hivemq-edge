/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

import type { ValidationError } from './ValidationError';

export type FunctionMustBePairedValidationError = (ValidationError & {
    /**
     * The existing function.
     */
    existingFunction: string;
    /**
     * The missing function.
     */
    missingFunction: string;
});

