/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $FieldMappingsModel = {
    description: `List of result items that are returned by this endpoint`,
    properties: {
        fieldMapping: {
            type: 'array',
            contains: {
                type: 'FieldMappingModel',
            },
        },
        metadata: {
            type: 'FieldMappingMetaDataModel',
        },
        tag: {
            type: 'string',
        },
        topicFilter: {
            type: 'string',
            description: `The topic filter according to the MQTT specification.`,
            format: 'mqtt-topic-filter',
        },
    },
} as const;
