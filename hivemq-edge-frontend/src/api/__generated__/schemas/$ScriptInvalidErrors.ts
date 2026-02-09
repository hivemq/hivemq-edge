/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $ScriptInvalidErrors = {
    type: 'all-of',
    contains: [{
        type: 'ApiProblemDetails',
    }, {
        properties: {
            childErrors: {
                type: 'array',
                contains: {
                    type: 'ScriptValidationError',
                },
                isRequired: true,
            },
        },
    }],
} as const;
