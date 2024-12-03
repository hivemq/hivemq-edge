/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $SouthboundMapping = {
    description: `List of result items that are returned by this endpoint`,
    properties: {
        fieldMapping: {
            type: 'FieldMapping',
        },
        maxQoS: {
            type: 'number',
            description: `The maximum MQTT-QoS for the outgoing messages.`,
            isRequired: true,
            format: 'int32',
        },
        tagName: {
            type: 'string',
            description: `The tag for which values hould be collected and sent out.`,
            format: 'mqtt-tag',
        },
        topicFilter: {
            type: 'string',
            description: `The filter defining what topics we will receive messages from.`,
        },
    },
} as const;
