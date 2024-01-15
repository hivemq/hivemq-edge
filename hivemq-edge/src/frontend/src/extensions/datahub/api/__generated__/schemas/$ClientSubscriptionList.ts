/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $ClientSubscriptionList = {
    properties: {
        _links: {
            type: 'PaginationCursor',
        },
        items: {
            type: 'array',
            contains: {
                type: 'ClientSubscription',
            },
        },
    },
} as const;
