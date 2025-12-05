/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $InvalidQueryParameterError = {
    type: 'all-of',
    contains: [{
        type: 'ApiProblemDetails',
    }, {
        properties: {
            parameter: {
                type: 'string',
                description: `The query parameter.`,
                isRequired: true,
            },
        },
    }],
} as const;
