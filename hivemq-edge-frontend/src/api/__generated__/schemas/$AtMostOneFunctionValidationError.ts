/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $AtMostOneFunctionValidationError = {
    type: 'all-of',
    contains: [{
        type: 'ValidationError',
    }, {
        properties: {
            function: {
                type: 'string',
                description: `The function.`,
                isRequired: true,
            },
            occurrences: {
                type: 'number',
                description: `The occurrences of the function.`,
                isRequired: true,
                format: 'int32',
            },
            paths: {
                type: 'array',
                contains: {
                    type: 'string',
                    format: 'json-path',
                },
                isRequired: true,
            },
        },
    }],
} as const;
