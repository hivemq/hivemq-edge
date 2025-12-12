/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

import type { ValidationError } from './ValidationError';

export type InvalidFunctionOrderValidationError = (ValidationError & {
    /**
     * The function.
     */
    function: string;
    /**
     * The json path.
     */
    path: string;
    /**
     * The previous function.
     */
    previousFunction: string;
});

