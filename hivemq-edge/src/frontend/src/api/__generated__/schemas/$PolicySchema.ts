/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $PolicySchema = {
    properties: {
        arguments: {
            type: 'dictionary',
            contains: {
                type: 'string',
                description: `The schema type dependent arguments.`,
            },
        },
        createdAt: {
            type: 'string',
            description: `The formatted UTC timestamp when the schema was created.`,
            isReadOnly: true,
        },
        id: {
            type: 'string',
            description: `The unique identifier of the schema.`,
            isRequired: true,
        },
        schemaDefinition: {
            type: 'string',
            description: `The base64 encoded schema definition.`,
            isRequired: true,
        },
        type: {
            type: 'string',
            description: `The type of the schema.`,
            isRequired: true,
        },
        version: {
            type: 'number',
            description: `The version of the schema.`,
            isReadOnly: true,
            format: 'int32',
        },
    },
} as const;
