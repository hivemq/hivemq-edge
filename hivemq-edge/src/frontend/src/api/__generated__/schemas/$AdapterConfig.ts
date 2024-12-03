/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $AdapterConfig = {
    properties: {
        config: {
            type: 'Adapter',
        },
        northboundMappings: {
            type: 'array',
            contains: {
                type: 'NorthboundMapping',
            },
        },
        southboundMappings: {
            type: 'array',
            contains: {
                type: 'SouthboundMapping',
            },
        },
        tags: {
            type: 'array',
            contains: {
                type: 'DomainTag',
            },
        },
    },
} as const;
