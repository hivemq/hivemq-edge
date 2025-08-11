/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $ManagedAsset = {
    type: 'all-of',
    description: `The definition of an extended asset, as managed in Edge for the Pulse Client`,
    contains: [{
        type: 'Asset',
    }, {
        properties: {
            mapping: {
                type: 'AssetMapping',
            },
        },
    }],
} as const;
