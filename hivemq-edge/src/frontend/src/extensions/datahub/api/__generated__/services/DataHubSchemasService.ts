/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { Schema } from '../models/Schema';
import type { SchemaList } from '../models/SchemaList';

import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';

export class DataHubSchemasService {

    constructor(public readonly httpRequest: BaseHttpRequest) {}

    /**
     * Get all schemas
     * Get all schemas.
     *
     * This endpoint returns the content of the schemas with the content-type `application/json`.
     *
     * This endpoint requires at least HiveMQ version 4.16.0 on all cluster nodes.
     * @param fields Comma-separated list of fields to include in the response. Allowed values are: id, type, schemaDefinition, createdAt
     * @param types Comma-separated list of schema types used for filtering. Multiple filters can be applied together.
     * @param schemaIds Comma-separated list of schema ids used for filtering. Multiple filters can be applied together.
     * @param limit Specifies the page size for the returned results. Has to be between 10 and 500. Default page size is 50.
     * @param cursor The cursor that has been returned by the previous result page. Do not pass this parameter if you want to fetch the first page.
     * @returns SchemaList Success
     * @throws ApiError
     */
    public getAllSchemas(
        fields?: string,
        types?: string,
        schemaIds?: string,
        limit?: number,
        cursor?: string,
    ): CancelablePromise<SchemaList> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/data-hub/schemas',
            query: {
                'fields': fields,
                'types': types,
                'schemaIds': schemaIds,
                'limit': limit,
                'cursor': cursor,
            },
            errors: {
                503: `Not all cluster nodes at minimum version`,
            },
        });
    }

    /**
     * Create a new schema
     * Creates a schema
     *
     * This endpoint requires at least HiveMQ version 4.15.0 on all cluster nodes.
     * @param requestBody The schema that should be created.
     * @returns Schema Success
     * @throws ApiError
     */
    public createSchema(
        requestBody: Schema,
    ): CancelablePromise<Schema> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/v1/data-hub/schemas',
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                400: `Bad request`,
                503: `Not all cluster nodes at minimum version`,
            },
        });
    }

    /**
     * Delete all versions of the schema
     * Deletes the selected schema and all associated versions of the schema.
     *
     * This endpoint requires HiveMQ version4.15.0 or above on all cluster nodes.
     * @param schemaId The schema identifier of the schema versions to delete.
     * @returns void
     * @throws ApiError
     */
    public deleteSchema(
        schemaId: string,
    ): CancelablePromise<void> {
        return this.httpRequest.request({
            method: 'DELETE',
            url: '/api/v1/data-hub/schemas/{schemaId}',
            path: {
                'schemaId': schemaId,
            },
            errors: {
                400: `Bad request`,
                404: `Resource not found`,
                503: `Not all cluster nodes at minimum version`,
            },
        });
    }

    /**
     * Get a schema
     * Get a specific schema.
     *
     * This endpoint returns the content of the latest version of the schema with the content-type `application/json`.
     *
     * This endpoint requires at least HiveMQ version 4.15.0 on all cluster nodes.
     * @param schemaId The identifier of the schema.
     * @param fields Comma-separated list of fields to include in the response. Allowed values are: id, type, schemaDefinition, createdAt
     * @returns Schema Success
     * @throws ApiError
     */
    public getSchema(
        schemaId: string,
        fields?: string,
    ): CancelablePromise<Schema> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/data-hub/schemas/{schemaId}',
            path: {
                'schemaId': schemaId,
            },
            query: {
                'fields': fields,
            },
            errors: {
                400: `Bad request`,
                404: `Resource not found`,
            },
        });
    }

}
