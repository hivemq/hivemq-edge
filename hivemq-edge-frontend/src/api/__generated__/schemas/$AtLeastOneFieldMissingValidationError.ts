/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $AtLeastOneFieldMissingValidationError = {
    type: 'all-of',
    contains: [{
        type: 'ValidationError',
    }, {
        properties: {
            paths: {
                type: 'array',
                contains: {
                    type: 'string',
                    description: `The json path.`,
                    format: 'json-path',
                },
                isRequired: true,
            },
        },
    }],
} as const;
