/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

import type { ValidationError } from './ValidationError';

export type InvalidFieldLengthValidationError = (ValidationError & {
    /**
     * The actual length of the field value.
     */
    actualLength: number;
    /**
     * The minimum length expected for the field value.
     */
    expectedMinimumLength: number;
    /**
     * The maximum length expected for the field value.
     */
    expectedMaximumLength: number;
    /**
     * The invalid json path.
     */
    path: string;
    /**
     * The invalid value.
     */
    value: string;
});

