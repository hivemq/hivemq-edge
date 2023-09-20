/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $ProtocolAdapterCategory = {
    description: `The category of the adapter`,
    properties: {
        description: {
            type: 'string',
            description: `The description associated with the category.`,
            format: 'string',
        },
        displayName: {
            type: 'string',
            description: `The display name of the category to be used in HCIs.`,
            isRequired: true,
            format: 'string',
            minLength: 1,
        },
        image: {
            type: 'string',
            description: `The image associated with the category.`,
            format: 'string',
        },
        name: {
            type: 'string',
            description: `The unique name of the category to be used in API communication.`,
            isRequired: true,
            format: 'string',
            maxLength: 256,
            minLength: 1,
            pattern: '^[A-Za-z0-9-_](?:[A-Za-z0-9_ -]*[A-Za-z0-9_-])$',
        },
    },
} as const;
