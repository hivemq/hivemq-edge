/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

export type StatusTransitionResult = {
    /**
     * The callback timeout specifies the minimum amount of time (in milliseconds) that the API advises the client to backoff before rechecking the (runtime or connection) status of this object. This is only applicable when the status is 'PENDING'.
     */
    callbackTimeoutMillis?: number;
    /**
     * The identifier of the object in transition
     */
    identifier?: string;
    /**
     * The status to perform on the target connection.
     */
    status?: StatusTransitionResult.status;
    /**
     * The type of the object in transition
     */
    type?: string;
};

export namespace StatusTransitionResult {

    /**
     * The status to perform on the target connection.
     */
    export enum status {
        PENDING = 'PENDING',
        COMPLETE = 'COMPLETE',
    }


}

