/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $SchemaParsingFailureError = {
    type: 'all-of',
    contains: [{
        type: 'ApiProblemDetails',
    }, {
        properties: {
            alias: {
                type: 'string',
                description: `The schema alias.`,
                isRequired: true,
            },
        },
    }],
} as const;
