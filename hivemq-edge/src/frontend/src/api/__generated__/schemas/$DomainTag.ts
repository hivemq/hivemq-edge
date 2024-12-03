/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $DomainTag = {
    description: `List of result items that are returned by this endpoint`,
    properties: {
        definition: {
            type: 'JsonNode',
            isRequired: true,
        },
        description: {
            type: 'string',
            description: `A user created description for this tag.`,
        },
        name: {
            type: 'string',
            description: `The name of the tag that identifies it within this edge instance.`,
            isRequired: true,
            format: 'mqtt-tag',
        },
    },
} as const;
