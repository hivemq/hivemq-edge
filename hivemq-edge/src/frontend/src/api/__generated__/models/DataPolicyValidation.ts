/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

import type { DataPolicyValidator } from './DataPolicyValidator';

/**
 * The section of the policy that defines how incoming MQTT messages are validated. If this section is empty, the result of the policy validation is always successful.
 */
export type DataPolicyValidation = {
    /**
     * The validators of the policy.
     */
    validators?: Array<DataPolicyValidator>;
};

