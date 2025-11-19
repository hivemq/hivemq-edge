/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $DomainTagOwnerList = {
    properties: {
        items: {
            type: 'array',
            contains: {
                properties: {
                    adapterId: {
                        type: 'string',
                        description: `The id of the adapter owning the tag`,
                        isRequired: true,
                    },
                    mapping: {
                        type: 'DomainTag',
                        isRequired: true,
                    },
                },
            },
            isRequired: true,
        },
    },
} as const;
