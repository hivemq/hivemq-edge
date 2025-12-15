/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $SchemaReferencedError = {
    type: 'all-of',
    contains: [{
        type: 'ApiProblemDetails',
    }, {
        properties: {
            id: {
                type: 'string',
                description: `The schema ID.`,
                isRequired: true,
            },
        },
    }],
} as const;
