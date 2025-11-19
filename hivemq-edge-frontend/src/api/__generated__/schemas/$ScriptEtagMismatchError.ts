/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $ScriptEtagMismatchError = {
    type: 'all-of',
    contains: [{
        type: 'ApiProblemDetails',
    }, {
        properties: {
            id: {
                type: 'string',
                description: `The script id.`,
                isRequired: true,
            },
            eTag: {
                type: 'string',
                description: `The eTag.`,
            },
        },
    }],
} as const;
