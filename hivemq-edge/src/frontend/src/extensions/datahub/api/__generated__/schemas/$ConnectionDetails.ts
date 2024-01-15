/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $ConnectionDetails = {
    description: `Information about the clients connection`,
    properties: {
        cleanStart: {
            type: 'boolean',
            description: `Clean start flag`,
        },
        connectedListenerId: {
            type: 'string',
            description: `Id of the HiveMQ listener the client is connected to`,
        },
        connectedNodeId: {
            type: 'string',
            description: `Id of the HiveMQ node the client is connected to`,
        },
        keepAlive: {
            type: 'number',
            description: `Connection Keep Alive in seconds`,
            isNullable: true,
            format: 'int32',
        },
        mqttVersion: {
            type: 'string',
            description: `MQTT version of the client`,
        },
        password: {
            type: 'string',
            description: `Password`,
            isNullable: true,
            format: 'byte',
        },
        proxyInformation: {
            type: 'ProxyInformation',
        },
        sourceIp: {
            type: 'string',
            description: `The client's IP`,
            isNullable: true,
        },
        tlsInformation: {
            type: 'TlsInformation',
        },
        username: {
            type: 'string',
            description: `Username`,
            isNullable: true,
        },
    },
    isNullable: true,
} as const;
