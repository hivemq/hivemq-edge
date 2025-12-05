/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

import type { ApiProblemDetails } from './ApiProblemDetails';

export type PolicyIdMismatchError = (ApiProblemDetails & {
    /**
     * The actual id.
     */
    actualId: string;
    /**
     * The expected id.
     */
    expectedId: string;
});

