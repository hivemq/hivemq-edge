/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $ObjectNode = {
    description: `List of result items that are returned by this endpoint`,
    properties: {
        children: {
            type: 'array',
            contains: {
                type: 'ObjectNode',
            },
        },
        description: {
            type: 'string',
        },
        id: {
            type: 'string',
        },
        name: {
            type: 'string',
        },
        nodeType: {
            type: 'Enum',
        },
        selectable: {
            type: 'boolean',
        },
    },
} as const;
