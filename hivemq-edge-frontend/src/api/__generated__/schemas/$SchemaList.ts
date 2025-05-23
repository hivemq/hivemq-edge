/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $SchemaList = {
    description: `A listing of schemas.`,
    properties: {
        _links: {
            type: 'PaginationCursor',
        },
        items: {
            type: 'array',
            contains: {
                type: 'PolicySchema',
            },
        },
    },
} as const;
