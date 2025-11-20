/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $DataPolicyInvalidErrors = {
    type: 'all-of',
    contains: [{
        type: 'ApiProblemDetails',
    }, {
        properties: {
            childErrors: {
                type: 'array',
                contains: {
                    type: 'DataPolicyValidationError',
                },
                isRequired: true,
            },
        },
    }],
} as const;
