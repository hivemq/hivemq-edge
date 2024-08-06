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
            description: `The server path from the config`,
        },
        subProtocol: {
            type: 'string',
            description: `The subProtocol from the config`,
        },
    },
    isNullable: true,
} as const;
