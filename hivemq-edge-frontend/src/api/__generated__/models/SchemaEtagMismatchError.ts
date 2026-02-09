/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

import type { ApiProblemDetails } from './ApiProblemDetails';

export type SchemaEtagMismatchError = (ApiProblemDetails & {
    /**
     * The schema id.
     */
    id: string;
    /**
     * The eTag.
     */
    eTag?: string;
});

