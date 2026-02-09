/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $RequestBodyParameterMissingError = {
    type: 'all-of',
    contains: [{
        type: 'ApiProblemDetails',
    }, {
        properties: {
            parameter: {
                type: 'string',
                description: `The the missing request body parameter.`,
                isRequired: true,
            },
        },
    }],
} as const;
