/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $ManagedAssetList = {
    description: `A list of managed assets, as managed in Edge for the Pulse Client`,
    properties: {
        items: {
            type: 'array',
            contains: {
                type: 'ManagedAsset',
            },
            isRequired: true,
        },
    },
} as const;
