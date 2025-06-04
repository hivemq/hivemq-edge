/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $Bridge = {
    properties: {
        cleanStart: {
            type: 'boolean',
            description: `The cleanStart value associated the the MQTT connection.`,
            isRequired: true,
            format: 'boolean',
        },
        clientId: {
            type: 'string',
            description: `The client identifier associated the the MQTT connection.`,
            isNullable: true,
            format: 'string',
            maxLength: 65535,
        },
        host: {
            type: 'string',
            description: `The host the bridge connects to - a well formed hostname, ipv4 or ipv6 value.`,
            isRequired: true,
            maxLength: 255,
        },
        id: {
            type: 'string',
            description: `The bridge id, must be unique and only contain alpha numeric characters with spaces and hyphens.`,
            isRequired: true,
            format: 'string',
            maxLength: 500,
            minLength: 1,
            pattern: '^([a-zA-Z_0-9-_])*$',
        },
        keepAlive: {
            type: 'number',
            description: `The keepAlive associated the the MQTT connection.`,
            isRequired: true,
            format: 'int32',
            maximum: 65535,
        },
        localSubscriptions: {
            type: 'array',
            contains: {
                type: 'LocalBridgeSubscription',
            },
        },
        loopPreventionEnabled: {
            type: 'boolean',
            description: `Is loop prevention enabled on the connection`,
            format: 'boolean',
        },
        loopPreventionHopCount: {
            type: 'number',
            description: `Loop prevention hop count`,
            format: 'int32',
            maximum: 100,
        },
        password: {
            type: 'string',
            description: `The password value associated the the MQTT connection.`,
            isNullable: true,
            format: 'string',
            maxLength: 65535,
        },
        persist: {
            type: 'boolean',
            description: `If this flag is set to true, any outgoing mqtt messages with QoS-1 or QoS-2 will be persisted on disc in case disc persistence is active.If this flag is set to false, the QoS of any outgoing mqtt messages will be set to QoS-0 and no traffic will be persisted on disc.`,
            isNullable: true,
        },
        port: {
            type: 'number',
            description: `The port number to connect to`,
            isRequired: true,
            format: 'int32',
            maximum: 65535,
            minimum: 1,
        },
        remoteSubscriptions: {
            type: 'array',
            contains: {
                type: 'BridgeSubscription',
            },
        },
        sessionExpiry: {
            type: 'number',
            description: `The sessionExpiry associated the the MQTT connection.`,
            isRequired: true,
            format: 'int64',
        },
        status: {
            type: 'Status',
        },
        tlsConfiguration: {
            type: 'TlsConfiguration',
        },
        username: {
            type: 'string',
            description: `The username value associated the the MQTT connection.`,
            isNullable: true,
            format: 'string',
            maxLength: 65535,
        },
        websocketConfiguration: {
            type: 'WebsocketConfiguration',
        },
    },
} as const;
