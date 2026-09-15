/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $ImportResult = {
    description: `Result of a bulk device tag import operation.`,
    properties: {
        tagsCreated: {
            type: 'number',
            description: `Number of new tags created.`,
        },
        tagsUpdated: {
            type: 'number',
            description: `Number of existing tags overwritten.`,
        },
        tagsDeleted: {
            type: 'number',
            description: `Number of tags removed.`,
        },
        northboundMappingsCreated: {
            type: 'number',
            description: `Number of northbound mappings created.`,
        },
        northboundMappingsDeleted: {
            type: 'number',
            description: `Number of northbound mappings removed.`,
        },
        southboundMappingsCreated: {
            type: 'number',
            description: `Number of southbound mappings created.`,
        },
        southboundMappingsDeleted: {
            type: 'number',
            description: `Number of southbound mappings removed.`,
        },
        tagActions: {
            type: 'array',
            contains: {
                type: 'TagAction',
            },
        },
    },
} as const;
