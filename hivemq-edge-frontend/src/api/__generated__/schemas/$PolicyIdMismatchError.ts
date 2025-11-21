/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $PolicyIdMismatchError = {
    type: 'all-of',
    contains: [{
        type: 'ApiProblemDetails',
    }, {
        properties: {
            actualId: {
                type: 'string',
                description: `The actual id.`,
                isRequired: true,
            },
            expectedId: {
                type: 'string',
                description: `The expected id.`,
                isRequired: true,
            },
        },
    }],
} as const;
