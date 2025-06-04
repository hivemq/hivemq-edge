/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

import type { JsonNode } from './JsonNode';

/**
 * List of result items that are returned by this endpoint
 */
export type FsmStateInformationItem = {
    arguments?: JsonNode;
    /**
     * The unique identifier of the policy.
     */
    behaviorId?: string;
    /**
     * The timestamp when this state was set the first time.
     */
    firstSetAt?: string;
    /**
     * The unique identifier of the policy.
     */
    policyId?: string;
    /**
     * The name of the fsm state.
     */
    stateName?: string;
    /**
     * The type of the fsm state.
     */
    stateType?: string;
    /**
     * The variables for this fsm.
     */
    variables?: Record<string, string>;
};

