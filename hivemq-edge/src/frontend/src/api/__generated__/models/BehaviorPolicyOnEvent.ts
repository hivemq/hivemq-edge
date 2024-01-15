/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

import type { PolicyOperation } from './PolicyOperation';

/**
 * One or more operations that are triggered on the event. When this field is empty, the transition does not trigger any operations.
 */
export type BehaviorPolicyOnEvent = {
    pipeline: Array<PolicyOperation>;
};

