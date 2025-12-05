/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $IllegalFunctionValidationError = {
    type: 'all-of',
    contains: [{
        type: 'ValidationError',
    }, {
        properties: {
            event: {
                type: 'string',
                description: `The event name.`,
                isRequired: true,
            },
            id: {
                type: 'string',
                description: `The function id.`,
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
