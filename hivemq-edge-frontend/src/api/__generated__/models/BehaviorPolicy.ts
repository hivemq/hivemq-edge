/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

import type { BehaviorPolicyBehavior } from './BehaviorPolicyBehavior';
import type { BehaviorPolicyDeserialization } from './BehaviorPolicyDeserialization';
import type { BehaviorPolicyMatching } from './BehaviorPolicyMatching';
import type { BehaviorPolicyOnTransition } from './BehaviorPolicyOnTransition';

/**
 * A policy which is used to validate and execute certain actions based on the validation result.
 */
export type BehaviorPolicy = {
    behavior: BehaviorPolicyBehavior;
    /**
     * The formatted UTC timestamp indicating when the policy was created.
     */
    readonly createdAt?: string;
    deserialization?: BehaviorPolicyDeserialization;
    /**
     * The unique identifier of the policy.
     */
    id: string;
    /**
     * The formatted UTC timestamp indicating when the policy was updated the last time.
     */
    readonly lastUpdatedAt?: string;
    matching: BehaviorPolicyMatching;
    onTransitions?: Array<BehaviorPolicyOnTransition>;
};

