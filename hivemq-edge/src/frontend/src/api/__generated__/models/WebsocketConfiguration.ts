/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

/**
 * websocketConfiguration associated with the bridge
 */
export type WebsocketConfiguration = {
    /**
     * If Websockets are used
     */
    enabled?: boolean;
    /**
     * The server path used by the bridge client. This must be setup as path at the remote broker
     */
    serverPath?: string;
    /**
     * The sub-protocol used by the bridge client. This must be supported by the remote broker
     */
    subProtocol?: string;
};

