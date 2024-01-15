/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $SchemaReference = {
    description: `A schema reference is a unique identifier for a schema.`,
    properties: {
        schemaId: {
            type: 'string',
            description: `The identifier of the schema.`,
            isRequired: true,
        },
        version: {
            type: 'string',
            description: `The version of the schema. The value "latest" may be used to always refer to the latest schema.`,
            isRequired: true,
        },
    },
} as const;
