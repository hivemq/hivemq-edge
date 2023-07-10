/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $AdapterRuntimeInformation = {
    description: `Information associated with the runtime of this adapter`,
    properties: {
        connectionStatus: {
            type: 'ConnectionStatus',
        },
        errorMessage: {
            type: 'string',
            description: `An error message associated with the connection`,
        },
        lastStartedAttemptTime: {
            type: 'string',
            description: `Time last start attempt time`,
            format: 'date-time',
        },
        numberOfDaemonProcesses: {
            type: 'number',
            description: `The number of daemon processes associated with this instance`,
            format: 'int32',
        },
    },
} as const;
