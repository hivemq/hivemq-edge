/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $Module = {
    description: `List of result items that are returned by this endpoint`,
    properties: {
        author: {
            type: 'string',
            description: `The module author`,
        },
        description: {
            type: 'string',
            description: `The module description`,
            isNullable: true,
        },
        documentationLink: {
            type: 'Link',
        },
        id: {
            type: 'string',
            description: `A mandatory ID associated with the Module`,
        },
        installed: {
            type: 'boolean',
            description: `Is the module installed`,
            isNullable: true,
        },
        logoUrl: {
            type: 'Link',
        },
        moduleType: {
            type: 'string',
            description: `The type of the module`,
            isNullable: true,
        },
        name: {
            type: 'string',
            description: `The module name`,
        },
        priority: {
            type: 'number',
            description: `The module priority`,
            format: 'int32',
        },
        provisioningLink: {
            type: 'Link',
        },
        version: {
            type: 'string',
            description: `The module version`,
        },
    },
} as const;
