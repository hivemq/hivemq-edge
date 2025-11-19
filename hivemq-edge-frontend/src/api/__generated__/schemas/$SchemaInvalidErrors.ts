/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $SchemaInvalidErrors = {
    type: 'all-of',
    contains: [{
        type: 'ApiProblemDetails',
    }, {
        properties: {
            childErrors: {
                type: 'array',
                contains: {
                    type: 'SchemaValidationError',
                },
                isRequired: true,
            },
        },
    }],
} as const;
