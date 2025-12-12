/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

import type { ApiProblemDetails } from './ApiProblemDetails';

export type RequestBodyParameterMissingError = (ApiProblemDetails & {
    /**
     * The the missing request body parameter.
     */
    parameter: string;
});

