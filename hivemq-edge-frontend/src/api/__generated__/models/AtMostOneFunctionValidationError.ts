/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

import type { ValidationError } from './ValidationError';

export type AtMostOneFunctionValidationError = (ValidationError & {
    /**
     * The function.
     */
    function: string;
    /**
     * The occurrences of the function.
     */
    occurrences: number;
    /**
     * The json paths where the function occurs.
     */
    paths: Array<string>;
});

