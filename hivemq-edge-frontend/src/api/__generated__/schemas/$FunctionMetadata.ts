/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $FunctionMetadata = {
    description: `Metadata for operation functions`,
    properties: {
        isTerminal: {
            type: 'boolean',
            description: `The function is a terminal element of a pipeline`,
        },
        isDataOnly: {
            type: 'boolean',
            description: `The function is only available for Data Policies`,
        },
        hasArguments: {
            type: 'boolean',
            description: `The function has extra arguments`,
        },
        inLicenseAllowed: {
            type: 'boolean',
            description: `The function can be used with the current user's license`,
        },
        supportedEvents: {
            type: 'array',
            contains: {
                type: 'BehaviorPolicyTransitionEvent',
            },
        },
    },
} as const;
