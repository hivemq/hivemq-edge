/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

import type { BehaviorPolicyOnEvent } from './BehaviorPolicyOnEvent';

/**
 * The actions that are executed for the specified transition.
 */
export type BehaviorPolicyOnTransition = {
    'Connection.OnDisconnect'?: BehaviorPolicyOnEvent;
    'Event.OnAny'?: BehaviorPolicyOnEvent;
    'Mqtt.OnInboundConnect'?: BehaviorPolicyOnEvent;
    'Mqtt.OnInboundDisconnect'?: BehaviorPolicyOnEvent;
    'Mqtt.OnInboundPublish'?: BehaviorPolicyOnEvent;
    'Mqtt.OnInboundSubscribe'?: BehaviorPolicyOnEvent;
    /**
     * The exact state from which the transition happened. Alternatively a state filter can be used.
     */
    fromState: string;
    /**
     * The exact state to which the transition happened. Alternatively a state filter can be used.
     */
    toState: string;
};

