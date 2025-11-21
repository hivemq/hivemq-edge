/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $MissingFieldValidationError = {
    type: 'all-of',
    contains: [{
        type: 'ValidationError',
    }, {
        properties: {
            path: {
                type: 'string',
                description: `The missing path.`,
                isRequired: true,
                format: 'json-path',
            },
        },
    }],
} as const;
