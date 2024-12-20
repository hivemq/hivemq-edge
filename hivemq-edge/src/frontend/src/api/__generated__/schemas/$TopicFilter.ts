/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $TopicFilter = {
    description: `List of result items that are returned by this endpoint`,
    properties: {
        description: {
            type: 'string',
            description: `The name for this topic filter.`,
        },
        schema: {
            type: 'string',
            description: `The optional json schema for this topic filter in the data uri format.`,
            format: 'data-url',
        },
        topicFilter: {
            type: 'string',
            description: `The topic filter according to the MQTT specification.`,
            isRequired: true,
            format: 'mqtt-topic-filter',
        },
    },
} as const;
