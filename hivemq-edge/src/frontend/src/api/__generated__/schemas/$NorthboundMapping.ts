/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $NorthboundMapping = {
    description: `List of result items that are returned by this endpoint`,
    properties: {
        includeTagNames: {
            type: 'boolean',
            description: `Should tag names be included when sent out.`,
            isRequired: true,
        },
        includeTimestamp: {
            type: 'boolean',
            description: `Should the timestamp be included when sent out.`,
            isRequired: true,
        },
        maxQoS: {
            type: 'QoS',
            isRequired: true,
        },
        messageExpiryInterval: {
            type: 'number',
            description: `The message expiry interval.`,
            isRequired: true,
            format: 'int64',
        },
        tagName: {
            type: 'string',
            description: `The tag for which values hould be collected and sent out.`,
            isRequired: true,
            format: 'mqtt-tag',
        },
        topic: {
            type: 'string',
            description: `The target mqtt topic where received tags should be sent to.`,
            isRequired: true,
        },
        userProperties: {
            type: 'array',
            contains: {
                type: 'MqttUserProperty',
            },
        },
    },
} as const;
