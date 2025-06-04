/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

import type { Error } from './Error';

export type ProblemDetails = {
    /**
     * Correlation id
     */
    code?: string;
    detail?: string;
    errors?: Array<Error>;
    status?: number;
    title: string;
    type?: string;
};

