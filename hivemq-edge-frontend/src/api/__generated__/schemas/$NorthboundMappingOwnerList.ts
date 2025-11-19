/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $NorthboundMappingOwnerList = {
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
                        type: 'NorthboundMapping',
                        isRequired: true,
                    },
                },
            },
            isRequired: true,
        },
    },
} as const;
