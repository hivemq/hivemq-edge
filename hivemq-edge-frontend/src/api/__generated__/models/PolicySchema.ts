/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

export type PolicySchema = {
    /**
     * The schema type dependent arguments.
     */
    arguments?: Record<string, string>;
    /**
     * The formatted UTC timestamp when the schema was created.
     */
    readonly createdAt?: string;
    /**
     * The unique identifier of the schema.
     */
    id: string;
    /**
     * The base64 encoded schema definition.
     */
    schemaDefinition: string;
    /**
     * The type of the schema.
     */
    type: string;
    /**
     * The version of the schema.
     */
    readonly version?: number;
};

