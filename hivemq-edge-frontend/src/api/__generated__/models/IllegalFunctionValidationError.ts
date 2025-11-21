/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

import type { ValidationError } from './ValidationError';

export type IllegalFunctionValidationError = (ValidationError & {
    /**
     * The event name.
     */
    event: string;
    /**
     * The function id.
     */
    id: string;
    /**
     * The json path.
     */
    path: string;
});

