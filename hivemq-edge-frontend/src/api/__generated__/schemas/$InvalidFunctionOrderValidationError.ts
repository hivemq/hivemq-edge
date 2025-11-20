/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $InvalidFunctionOrderValidationError = {
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
            path: {
                type: 'string',
                description: `The json path.`,
                isRequired: true,
                format: 'json-path',
            },
            previousFunction: {
                type: 'string',
                description: `The previous function.`,
                isRequired: true,
            },
        },
    }],
} as const;
