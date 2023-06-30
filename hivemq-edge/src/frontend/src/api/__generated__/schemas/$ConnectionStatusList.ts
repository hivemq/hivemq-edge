/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $ConnectionStatusList = {
    properties: {
        items: {
            type: 'array',
            contains: {
                type: 'ConnectionStatus',
            },
        },
    },
} as const;
