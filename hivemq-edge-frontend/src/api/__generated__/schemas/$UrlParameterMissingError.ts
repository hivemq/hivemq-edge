/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $UrlParameterMissingError = {
    type: 'all-of',
    contains: [{
        type: 'ApiProblemDetails',
    }, {
        properties: {
            parameter: {
                type: 'string',
                description: `The name of the missing parameter.`,
                isRequired: true,
            },
        },
    }],
} as const;
