/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $ClientDetails = {
    properties: {
        connected: {
            type: 'boolean',
            description: `If this client is connected`,
        },
        connectedAt: {
            type: 'string',
            description: `Time the client connection was established`,
            isNullable: true,
            format: 'date-time',
        },
        connection: {
            type: 'ConnectionDetails',
        },
        id: {
            type: 'string',
            description: `The MQTT client identifier`,
        },
        messageQueueSize: {
            type: 'number',
            description: `The current message queue size for this client`,
            format: 'int64',
        },
        restrictions: {
            type: 'ClientRestrictions',
        },
        sessionExpiryInterval: {
            type: 'number',
            description: `The session expiry interval`,
            isNullable: true,
            format: 'int64',
        },
        willPresent: {
            type: 'boolean',
            description: `If a will is present for this client`,
        },
    },
} as const;
