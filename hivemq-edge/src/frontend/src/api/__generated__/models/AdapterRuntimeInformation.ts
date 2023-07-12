/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

import type { ConnectionStatus } from './ConnectionStatus';

/**
 * Information associated with the runtime of this adapter
 */
export type AdapterRuntimeInformation = {
    connectionStatus?: ConnectionStatus;
    /**
     * An error message associated with the connection
     */
    errorMessage?: string;
    /**
     * Time last start attempt time
     */
    lastStartedAttemptTime?: string;
    /**
     * The number of daemon processes associated with this instance
     */
    numberOfDaemonProcesses?: number;
};

