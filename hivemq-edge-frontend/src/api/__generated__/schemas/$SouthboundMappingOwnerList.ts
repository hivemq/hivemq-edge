/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $SouthboundMappingOwnerList = {
    properties: {
        items: {
            type: 'array',
            contains: {
                properties: {
                    adapterId: {
                        type: 'string',
                        description: `The id of the adapter owning the mapping`,
                        isRequired: true,
                    },
                    mapping: {
                        type: 'SouthboundMapping',
                        isRequired: true,
                    },
                },
            },
            isRequired: true,
        },
    },
} as const;
