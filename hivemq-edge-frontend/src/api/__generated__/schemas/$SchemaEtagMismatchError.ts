/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $SchemaEtagMismatchError = {
    type: 'all-of',
    contains: [{
        type: 'ApiProblemDetails',
    }, {
        properties: {
            id: {
                type: 'string',
                description: `The schema id.`,
                isRequired: true,
            },
            eTag: {
                type: 'string',
                description: `The eTag.`,
            },
        },
    }],
} as const;
