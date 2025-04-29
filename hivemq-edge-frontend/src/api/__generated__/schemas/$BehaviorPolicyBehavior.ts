/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $BehaviorPolicyBehavior = {
    description: `The behavior referenced by the policy, that is validated by the policy.`,
    properties: {
        arguments: {
            type: 'dictionary',
            contains: {
                properties: {
                },
            },
        },
        id: {
            type: 'string',
            description: `The unique identifier of a pre-defined behavior.`,
            isRequired: true,
        },
    },
} as const;
