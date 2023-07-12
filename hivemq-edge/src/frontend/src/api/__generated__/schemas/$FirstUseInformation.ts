/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $FirstUseInformation = {
    description: `Information relating to the firstuse experience`,
    properties: {
        firstUse: {
            type: 'boolean',
            description: `A mandatory Boolean indicating if the gateway is in firstUse mode`,
            isRequired: true,
        },
        firstUseDescription: {
            type: 'string',
            description: `A description string to use when firstUse = true.`,
            isNullable: true,
        },
        firstUseTitle: {
            type: 'string',
            description: `A header string to use when firstUse = true.`,
            isNullable: true,
        },
        prefillPassword: {
            type: 'string',
            description: `A String indicating if the prefill data for the username/password page.`,
            isNullable: true,
        },
        prefillUsername: {
            type: 'string',
            description: `A String indicating if the prefill data for the username/password page.`,
            isNullable: true,
        },
    },
} as const;
