/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $DomainTagOwner = {
    type: 'all-of',
    contains: [{
        type: 'DomainTag',
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
