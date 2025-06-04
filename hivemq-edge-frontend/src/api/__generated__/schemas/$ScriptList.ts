/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $ScriptList = {
    description: `A listing of scripts.`,
    properties: {
        _links: {
            type: 'PaginationCursor',
        },
        items: {
            type: 'array',
            contains: {
                type: 'Script',
            },
        },
    },
} as const;
