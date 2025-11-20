/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

import type { ValidationError } from './ValidationError';

export type UnsupportedFieldValidationError = (ValidationError & {
    /**
     * The actual value.
     */
    actualValue: string;
    /**
     * The expected value.
     */
    expectedValue: string;
    /**
     * The json path.
     */
    path: string;
});

