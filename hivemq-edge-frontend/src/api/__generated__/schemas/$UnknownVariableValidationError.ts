/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $UnknownVariableValidationError = {
    type: 'all-of',
    contains: [{
        type: 'ValidationError',
    }, {
        properties: {
            path: {
                type: 'string',
                description: `The json path of the field.`,
                isRequired: true,
            },
            variables: {
                type: 'array',
                contains: {
                    type: 'string',
                },
                isRequired: true,
            },
        },
    }],
} as const;
