/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $NorthboundMappingOwner = {
    type: 'all-of',
    contains: [{
        type: 'NorthboundMapping',
    }, {
        properties: {
            adapterId: {
                type: 'string',
                description: `The id of the adapter owning the tag`,
                isRequired: true,
            },
        },
    }],
} as const;
