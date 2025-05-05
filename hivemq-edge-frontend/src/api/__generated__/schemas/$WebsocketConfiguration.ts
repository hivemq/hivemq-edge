/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $WebsocketConfiguration = {
    description: `websocketConfiguration associated with the bridge`,
    properties: {
        enabled: {
            type: 'boolean',
            description: `If Websockets are used`,
        },
        serverPath: {
            type: 'string',
            description: `The server path used by the bridge client. This must be setup as path at the remote broker`,
        },
        subProtocol: {
            type: 'string',
            description: `The sub-protocol used by the bridge client. This must be supported by the remote broker`,
        },
    },
    isNullable: true,
} as const;
