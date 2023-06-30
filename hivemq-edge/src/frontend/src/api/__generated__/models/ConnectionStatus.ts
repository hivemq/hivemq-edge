/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

/**
 * The current status of the connection
 */
export type ConnectionStatus = {
    /**
     * The identifier of the object
     */
    id?: string;
    /**
     * A mandatory status field.
     */
    status?: ConnectionStatus.status;
    /**
     * The type of the object
     */
    type?: string;
};

export namespace ConnectionStatus {

    /**
     * A mandatory status field.
     */
    export enum status {
        CONNECTED = 'CONNECTED',
        DISCONNECTED = 'DISCONNECTED',
        CONNECTING = 'CONNECTING',
        DISCONNECTING = 'DISCONNECTING',
    }


}

