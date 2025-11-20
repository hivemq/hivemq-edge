/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

import type { ValidationError } from './ValidationError';

export type InvalidFieldValueValidationError = (ValidationError & {
    /**
     * The invalid json path.
     */
    path: string;
    /**
     * The invalid value.
     */
    value?: string;
});

