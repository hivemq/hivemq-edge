/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $ConnectionStatus = {
    description: `The current status of the connection`,
    properties: {
        id: {
            type: 'string',
            description: `The identifier of the object`,
        },
        status: {
            type: 'Enum',
        },
        type: {
            type: 'string',
            description: `The type of the object`,
        },
    },
} as const;
