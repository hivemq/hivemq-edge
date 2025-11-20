/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $UnsupportedFieldValidationError = {
    type: 'all-of',
    contains: [{
        type: 'ValidationError',
    }, {
        properties: {
            actualValue: {
                type: 'string',
                description: `The actual value.`,
                isRequired: true,
            },
            expectedValue: {
                type: 'string',
                description: `The expected value.`,
                isRequired: true,
            },
            path: {
                type: 'string',
                description: `The json path.`,
                isRequired: true,
                format: 'json-path',
            },
        },
    }],
} as const;
