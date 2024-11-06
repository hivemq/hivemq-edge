/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $ClientFilter = {
    description: `A client filter`,
    properties: {
        id: {
            type: 'string',
            description: `The unique id of the client filter`,
            isRequired: true,
        },
        topicFilters: {
            type: 'array',
            contains: {
                type: 'ClientFilterConfiguration',
            },
            isRequired: true,
        },
    },
} as const;
