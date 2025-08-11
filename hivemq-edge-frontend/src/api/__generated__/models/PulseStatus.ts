/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

import type { ProblemDetails } from './ProblemDetails';

/**
 * Information on the activation status of the pulse agent and its connection to the platform.
 */
export type PulseStatus = {
    activation: {
        /**
         * Status of the pulse activation
         */
        status: PulseStatus.activationStatus;
        message?: ProblemDetails;
    };
    runtime: {
        /**
         * Connection status of the pulse agent to the platform.
         */
        status: PulseStatus.runtimeStatus;
        message?: ProblemDetails;
    };
};

// TODO There is a bug with the generation of the enums (because of the same "status" property; replace manually
export namespace PulseStatus {

    /**
     * Status of the pulse activation
     */
    export enum activationStatus {
        ACTIVATED = 'ACTIVATED',
        DEACTIVATED = 'DEACTIVATED',
        ERROR = 'ERROR',
    }

    /**
     * Status of the pulse runtime
     */
    export enum runtimeStatus {
      CONNECTED = 'CONNECTED',
      DISCONNECTED = 'DISCONNECTED',
      ERROR = 'ERROR',
    }

}

