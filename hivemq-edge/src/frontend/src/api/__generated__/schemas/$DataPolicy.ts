/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $DataPolicy = {
    description: `A data policy which is used to validate and execute certain actions based on the validation result.`,
    properties: {
        createdAt: {
            type: 'string',
            description: `The formatted UTC timestamp indicating when the policy was created.`,
            isReadOnly: true,
            format: 'date-time',
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
            type: 'DataPolicyMatching',
            isRequired: true,
        },
        onFailure: {
            type: 'DataPolicyAction',
        },
        onSuccess: {
            type: 'DataPolicyAction',
        },
        validation: {
            type: 'DataPolicyValidation',
        },
    },
} as const;
