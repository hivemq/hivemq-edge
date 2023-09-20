/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $ProtocolAdapter = {
    description: `List of result items that are returned by this endpoint`,
    properties: {
        author: {
            type: 'string',
            description: `The author of the adapter`,
        },
        capabilities: {
            type: 'array',
            contains: {
                type: 'Enum',
            },
        },
        category: {
            type: 'ProtocolAdapterCategory',
        },
        configSchema: {
            type: 'JsonNode',
        },
        description: {
            type: 'string',
            description: `The description`,
        },
        id: {
            type: 'string',
            description: `The id assigned to the protocol adapter type`,
        },
        installed: {
            type: 'boolean',
            description: `Is the adapter installed?`,
        },
        logoUrl: {
            type: 'string',
            description: `The logo of the adapter`,
        },
        name: {
            type: 'string',
            description: `The name of the adapter`,
        },
        protocol: {
            type: 'string',
            description: `The supported protocol`,
        },
        provisioningUrl: {
            type: 'string',
            description: `The provisioning url of the adapter`,
        },
        tags: {
            type: 'array',
            contains: {
                type: 'string',
                description: `The search tags associated with this adapter`,
            },
        },
        url: {
            type: 'string',
            description: `The url of the adapter`,
        },
        version: {
            type: 'string',
            description: `The installed version of the adapter`,
        },
    },
} as const;
