/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

import type { ValidationError } from './ValidationError';

export type UnknownVariableValidationError = (ValidationError & {
    /**
     * The json path of the field.
     */
    path: string;
    /**
     * The unknown variables.
     */
    variables: Array<string>;
});

