/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

import type { ValidationError } from './ValidationError';

export type AtLeastOneFieldMissingValidationError = (ValidationError & {
    /**
     * The missing json paths.
     */
    paths: Array<string>;
});

