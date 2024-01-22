/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $Script = {
    properties: {
        createdAt: {
            type: 'string',
            description: `The formatted UTC timestamp when the script was created.`,
            isReadOnly: true,
        },
        description: {
            type: 'string',
            description: `A string of free-form text describing the function.`,
        },
        functionType: {
            type: 'Enum',
            isRequired: true,
        },
        id: {
            type: 'string',
            description: `The unique identifier of the script.`,
            isRequired: true,
        },
        source: {
            type: 'string',
            description: `The base64 encoded function source code.`,
            isRequired: true,
        },
        version: {
            type: 'number',
            description: `The version of the script.`,
            isReadOnly: true,
            format: 'int32',
        },
    },
} as const;
