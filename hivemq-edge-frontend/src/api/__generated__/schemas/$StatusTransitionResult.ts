/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $StatusTransitionResult = {
    properties: {
        callbackTimeoutMillis: {
            type: 'number',
            description: `The callback timeout specifies the minimum amount of time (in milliseconds) that the API advises the client to backoff before rechecking the (runtime or connection) status of this object. This is only applicable when the status is 'PENDING'.`,
            format: 'int32',
        },
        identifier: {
            type: 'string',
            description: `The identifier of the object in transition`,
        },
        status: {
            type: 'Enum',
        },
        type: {
            type: 'string',
            description: `The type of the object in transition`,
        },
    },
} as const;
