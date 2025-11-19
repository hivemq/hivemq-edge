/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $BehaviorPolicyNotFoundError = {
    type: 'all-of',
    contains: [{
        type: 'ApiProblemDetails',
    }, {
        properties: {
            id: {
                type: 'string',
                description: `The data policy id.`,
                isRequired: true,
            },
        },
    }],
} as const;
