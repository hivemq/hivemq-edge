/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $DomainTag = {
    description: `List of result items that are returned by this endpoint`,
    properties: {
        description: {
            type: 'string',
            description: `A user created description for this tag.`,
        },
        protocolId: {
            type: 'string',
            description: `The protocol id of the protocol for which this tag was created.`,
            isRequired: true,
        },
        tagDefinition: {
            type: 'JsonNode',
            isRequired: true,
        },
        tagName: {
            type: 'string',
            description: `The name of the tag that identifies it within this edge instance.`,
            isRequired: true,
            format: 'mqtt-tag',
        },
    },
} as const;
