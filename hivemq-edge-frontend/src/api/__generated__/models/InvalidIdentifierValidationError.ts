/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

import type { ValidationError } from './ValidationError';

export type InvalidIdentifierValidationError = (ValidationError & {
    /**
     * The invalid identifier path.
     */
    path: string;
    /**
     * The invalid identifier value.
     */
    value: string;
});

