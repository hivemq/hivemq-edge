/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $InvalidFieldLengthValidationError = {
    type: 'all-of',
    contains: [{
        type: 'ValidationError',
    }, {
        properties: {
            actualLength: {
                type: 'number',
                description: `The actual length of the field value.`,
                isRequired: true,
                format: 'int32',
            },
            expectedMinimumLength: {
                type: 'number',
                description: `The minimum length expected for the field value.`,
                isRequired: true,
                format: 'int32',
            },
            expectedMaximumLength: {
                type: 'number',
                description: `The maximum length expected for the field value.`,
                isRequired: true,
                format: 'int32',
            },
            path: {
                type: 'string',
                description: `The invalid json path.`,
                isRequired: true,
                format: 'json-path',
            },
            value: {
                type: 'string',
                description: `The invalid value.`,
                isRequired: true,
            },
        },
    }],
} as const;
