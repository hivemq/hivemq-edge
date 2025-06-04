/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $BehaviorPolicyList = {
    description: `A listing of behavior policies.`,
    properties: {
        _links: {
            type: 'PaginationCursor',
        },
        items: {
            type: 'array',
            contains: {
                type: 'BehaviorPolicy',
            },
        },
    },
} as const;
