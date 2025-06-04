/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $Link = {
    description: `An associated link`,
    properties: {
        description: {
            type: 'string',
            description: `The optional link display description`,
            isNullable: true,
        },
        displayText: {
            type: 'string',
            description: `The link display text`,
            isNullable: true,
        },
        external: {
            type: 'boolean',
            description: `A mandatory Boolean indicating if the link is internal to the context or an external webLink`,
        },
        imageUrl: {
            type: 'string',
            description: `An optional imageUrl associated with the Link`,
            isNullable: true,
        },
        target: {
            type: 'string',
            description: `An optional target associated with the Link`,
            isNullable: true,
        },
        url: {
            type: 'string',
            description: `A mandatory URL associated with the Link`,
            isRequired: true,
        },
    },
    isNullable: true,
} as const;
