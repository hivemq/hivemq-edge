/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $PolicyNotFoundError = {
    type: 'all-of',
    contains: [{
        type: 'ApiProblemDetails',
    }, {
        properties: {
            id: {
                type: 'string',
                description: `The policy id.`,
                isRequired: true,
            },
        },
    }],
} as const;
