/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

/**
 * Information associated with the runtime of this adapter
 */
export type Status = {
    /**
     * A mandatory connection status field.
     */
    connectionStatus?: Status.connectionStatus;
    /**
     * The identifier of the object
     */
    id?: string;
    /**
     * The datetime of the last activity through this connection
     */
    lastActivity?: string;
    /**
     * A message associated with the state of a connection
     */
    message?: string;
    /**
     * A object status field.
     */
    runtimeStatus?: Status.runtimeStatus;
    /**
     * The datetime of the last activity through this connection
     */
    startedAt?: string;
    /**
     * The type of the object
     */
    type?: string;
};

export namespace Status {

    /**
     * A mandatory connection status field.
     */
    export enum connectionStatus {
        CONNECTED = 'CONNECTED',
        DISCONNECTED = 'DISCONNECTED',
        STATELESS = 'STATELESS',
        UNKNOWN = 'UNKNOWN',
        ERROR = 'ERROR',
    }

    /**
     * A object status field.
     */
    export enum runtimeStatus {
        STARTED = 'STARTED',
        STOPPED = 'STOPPED',
    }


}

