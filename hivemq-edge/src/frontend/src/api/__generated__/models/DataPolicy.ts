/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

import type { DataPolicyAction } from './DataPolicyAction';
import type { DataPolicyMatching } from './DataPolicyMatching';
import type { DataPolicyValidation } from './DataPolicyValidation';

/**
 * A data policy which is used to validate and execute certain actions based on the validation result.
 */
export type DataPolicy = {
    /**
     * The formatted UTC timestamp indicating when the policy was created.
     */
    readonly createdAt?: string;
    /**
     * The unique identifier of the policy.
     */
    id: string;
    /**
     * The formatted UTC timestamp indicating when the policy was updated the last time.
     */
    readonly lastUpdatedAt?: string;
    matching: DataPolicyMatching;
    onFailure?: DataPolicyAction;
    onSuccess?: DataPolicyAction;
    validation?: DataPolicyValidation;
};

