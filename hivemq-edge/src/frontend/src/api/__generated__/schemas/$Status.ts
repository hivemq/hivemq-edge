/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $Status = {
    description: `Information associated with the runtime of this adapter`,
    properties: {
        connectionStatus: {
            type: 'Enum',
        },
        id: {
            type: 'string',
            description: `The identifier of the object`,
        },
        lastActivity: {
            type: 'string',
            description: `The datetime of the last activity through this connection`,
            format: 'date-time',
        },
        message: {
            type: 'string',
            description: `A message associated with the state of a connection`,
        },
        runtimeStatus: {
            type: 'Enum',
        },
        startedAt: {
            type: 'string',
            description: `The datetime of the last activity through this connection`,
            format: 'date-time',
        },
        type: {
            type: 'string',
            description: `The type of the object`,
        },
    },
} as const;
