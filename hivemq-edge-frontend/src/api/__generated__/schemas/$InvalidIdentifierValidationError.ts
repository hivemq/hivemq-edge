/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $InvalidIdentifierValidationError = {
    type: 'all-of',
    contains: [{
        type: 'ValidationError',
    }, {
        properties: {
            path: {
                type: 'string',
                description: `The invalid identifier path.`,
                isRequired: true,
                format: 'json-path',
            },
            value: {
                type: 'string',
                description: `The invalid identifier value.`,
                isRequired: true,
            },
        },
    }],
} as const;
