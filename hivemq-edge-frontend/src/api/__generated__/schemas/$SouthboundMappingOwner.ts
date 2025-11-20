/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $SouthboundMappingOwner = {
    type: 'all-of',
    contains: [{
        type: 'SouthboundMapping',
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
