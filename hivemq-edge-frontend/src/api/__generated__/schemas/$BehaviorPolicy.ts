/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $BehaviorPolicy = {
    description: `A policy which is used to validate and execute certain actions based on the validation result.`,
    properties: {
        behavior: {
            type: 'BehaviorPolicyBehavior',
            isRequired: true,
        },
        createdAt: {
            type: 'string',
            description: `The formatted UTC timestamp indicating when the policy was created.`,
            isReadOnly: true,
            format: 'date-time',
        },
        deserialization: {
            type: 'BehaviorPolicyDeserialization',
        },
        id: {
            type: 'string',
            description: `The unique identifier of the policy.`,
            isRequired: true,
        },
        lastUpdatedAt: {
            type: 'string',
            description: `The formatted UTC timestamp indicating when the policy was updated the last time.`,
            isReadOnly: true,
            format: 'date-time',
        },
        matching: {
            type: 'BehaviorPolicyMatching',
            isRequired: true,
        },
        onTransitions: {
            type: 'array',
            contains: {
                type: 'BehaviorPolicyOnTransition',
            },
        },
    },
} as const;
