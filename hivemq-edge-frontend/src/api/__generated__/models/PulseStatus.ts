/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

import type { ProblemDetails } from './ProblemDetails';

/**
 * Information on the activation status of the pulse agent and its connection to the platform.
 */
export type PulseStatus = {
    /**
     * Status of the pulse activation
     */
    activation: PulseStatus.activation;
    /**
     * Connection status of the pulse agent to the platform.
     */
    runtime: PulseStatus.runtime;
    message?: ProblemDetails;
};

export namespace PulseStatus {

    /**
     * Status of the pulse activation
     */
    export enum activation {
        ACTIVATED = 'ACTIVATED',
        DEACTIVATED = 'DEACTIVATED',
        ERROR = 'ERROR',
    }

    /**
     * Connection status of the pulse agent to the platform.
     */
    export enum runtime {
        CONNECTED = 'CONNECTED',
        DISCONNECTED = 'DISCONNECTED',
        ERROR = 'ERROR',
    }


}

