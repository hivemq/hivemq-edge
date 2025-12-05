/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $InvalidFieldValueValidationError = {
    type: 'all-of',
    contains: [{
        type: 'ValidationError',
    }, {
        properties: {
            path: {
                type: 'string',
                description: `The invalid json path.`,
                isRequired: true,
                format: 'json-path',
            },
            value: {
                type: 'string',
                description: `The invalid value.`,
            },
        },
    }],
} as const;
