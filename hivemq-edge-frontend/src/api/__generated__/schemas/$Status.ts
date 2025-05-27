/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $Status = {
    description: `Information associated with the runtime of this adapter`,
    properties: {
        connection: {
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
        runtime: {
            type: 'Enum',
        },
        startedAt: {
            type: 'string',
            description: `The datetime the object was 'started' in the system.`,
            format: 'date-time',
        },
        type: {
            type: 'string',
            description: `The type of the object`,
        },
    },
} as const;
