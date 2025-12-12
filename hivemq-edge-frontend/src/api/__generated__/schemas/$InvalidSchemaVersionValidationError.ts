/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $InvalidSchemaVersionValidationError = {
    type: 'all-of',
    contains: [{
        type: 'ValidationError',
    }, {
        properties: {
            id: {
                type: 'string',
                description: `The schema id.`,
                isRequired: true,
            },
            version: {
                type: 'string',
                description: `The schema version.`,
                isRequired: true,
            },
        },
    }],
} as const;
