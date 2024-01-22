/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $BehaviorPolicyOnTransition = {
    description: `The actions that are executed for the specified transition.`,
    properties: {
        'Connection.OnDisconnect': {
            type: 'BehaviorPolicyOnEvent',
        },
        'Event.OnAny': {
            type: 'BehaviorPolicyOnEvent',
        },
        'Mqtt.OnInboundConnect': {
            type: 'BehaviorPolicyOnEvent',
        },
        'Mqtt.OnInboundDisconnect': {
            type: 'BehaviorPolicyOnEvent',
        },
        'Mqtt.OnInboundPublish': {
            type: 'BehaviorPolicyOnEvent',
        },
        'Mqtt.OnInboundSubscribe': {
            type: 'BehaviorPolicyOnEvent',
        },
        fromState: {
            type: 'string',
            description: `The exact state from which the transition happened. Alternatively a state filter can be used.`,
            isRequired: true,
        },
        toState: {
            type: 'string',
            description: `The exact state to which the transition happened. Alternatively a state filter can be used.`,
            isRequired: true,
        },
    },
} as const;
