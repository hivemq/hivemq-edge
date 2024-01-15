/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $ClientRestrictions = {
    description: `The restrictions that are in effect for this client`,
    properties: {
        maxMessageSize: {
            type: 'number',
            description: `maximum MQTT message size`,
            isNullable: true,
            format: 'int64',
        },
        maxQueueSize: {
            type: 'number',
            description: `maximum queue size`,
            isNullable: true,
            format: 'int64',
        },
        queuedMessageStrategy: {
            type: 'string',
            description: `The queue strategy if the queue is full`,
            isNullable: true,
        },
    },
    isNullable: true,
} as const;
