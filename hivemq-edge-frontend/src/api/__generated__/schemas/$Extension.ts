/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $Extension = {
    description: `List of result items that are returned by this endpoint`,
    properties: {
        author: {
            type: 'string',
            description: `The extension author`,
        },
        description: {
            type: 'string',
            description: `The extension description`,
            isNullable: true,
        },
        id: {
            type: 'string',
            description: `A mandatory ID associated with the Extension`,
        },
        installed: {
            type: 'boolean',
            description: `Is the extension installed`,
            isNullable: true,
        },
        link: {
            type: 'Link',
        },
        name: {
            type: 'string',
            description: `The extension name`,
        },
        priority: {
            type: 'number',
            description: `The extension priority`,
            format: 'int32',
        },
        version: {
            type: 'string',
            description: `The extension version`,
        },
    },
} as const;
