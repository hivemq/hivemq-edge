/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $TopicFilterMismatchError = {
    type: 'all-of',
    contains: [{
        type: 'ApiProblemDetails',
    }, {
        properties: {
            path: {
                type: 'string',
                description: `The json path of the topic filter.`,
                isRequired: true,
            },
        },
    }],
} as const;
