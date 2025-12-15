/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $EmptyFieldValidationError = {
    type: 'all-of',
    contains: [{
        type: 'ValidationError',
    }, {
        properties: {
            path: {
                type: 'string',
                description: `The missing field.`,
                isRequired: true,
                format: 'json-path',
            },
        },
    }],
} as const;
