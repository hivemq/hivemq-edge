/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

import type { BehaviorPolicyDeserializer } from './BehaviorPolicyDeserializer';

/**
 * The deserializers used by the policy for particular message and/or payload types.
 */
export type BehaviorPolicyDeserialization = {
    publish?: BehaviorPolicyDeserializer;
    will?: BehaviorPolicyDeserializer;
};

