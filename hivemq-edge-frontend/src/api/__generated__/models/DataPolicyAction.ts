/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

import type { PolicyOperation } from './PolicyOperation';

/**
 * One or more operations the outcome of the validation triggers.  When this field is empty, the outcome of the policy validation does not trigger any operations.
 */
export type DataPolicyAction = {
    /**
     * The pipeline to execute, when this action is triggered. The operations in the pipeline are executed in-order.
     */
    pipeline?: Array<PolicyOperation>;
};

