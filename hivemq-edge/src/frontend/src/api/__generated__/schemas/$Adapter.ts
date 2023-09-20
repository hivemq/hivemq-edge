/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $Adapter = {
    properties: {
        config: {
            type: 'dictionary',
            contains: {
                type: 'dictionary',
                contains: {
                    properties: {
                    },
                },
            },
        },
        id: {
            type: 'string',
            description: `The adapter id, must be unique and only contain alpha numeric characters with spaces and hyphens.`,
            isRequired: true,
            format: 'string',
            maxLength: 500,
            minLength: 1,
            pattern: '^([a-zA-Z_0-9-_])*$',
        },
        status: {
            type: 'Status',
        },
        type: {
            type: 'string',
            description: `The adapter type associated with this instance`,
        },
    },
} as const;
