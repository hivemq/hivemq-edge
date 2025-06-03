/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

import type { BehaviorPolicyTransitionEvent } from './BehaviorPolicyTransitionEvent';

/**
 * Metadata for operation functions
 */
export type FunctionMetadata = {
    /**
     * The function is a terminal element of a pipeline
     */
    isTerminal?: boolean;
    /**
     * The function is only available for Data Policies
     */
    isDataOnly?: boolean;
    /**
     * The function has extra arguments
     */
    hasArguments?: boolean;
    /**
     * The function can be used with the current user's license
     */
    inLicenseAllowed?: boolean;
    supportedEvents?: Array<BehaviorPolicyTransitionEvent>;
};

