/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

import type { ConnectionStatus } from './ConnectionStatus';

/**
 * bridgeRuntimeInformation associated with the bridge
 */
export type BridgeRuntimeInformation = {
    connectionStatus?: ConnectionStatus;
    /**
     * An error message associated with the connection
     */
    errorMessage?: string;
};

