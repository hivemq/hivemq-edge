/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { Script } from '../models/Script';
import type { ScriptList } from '../models/ScriptList';

import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';

export class DataHubScriptsService {

    constructor(public readonly httpRequest: BaseHttpRequest) {}

    /**
     * Get all scripts
     * Get all scripts.
     * @param fields Comma-separated list of fields to include in the response. Allowed values are: id, version, description, runtime, functionType, createdAt
     * @param functionTypes Comma-separated list of function types used for filtering. Multiple filters can be applied together.
     * @param scriptIds Comma-separated list of script ids used for filtering. Multiple filters can be applied together.
     * @param limit Specifies the page size for the returned results. Has to be between 10 and 500. Default page size is 50.
     * @param cursor The cursor that has been returned by the previous result page. Do not pass this parameter if you want to fetch the first page.
     * @returns ScriptList Success
     * @throws ApiError
     */
    public getAllScripts(
        fields?: string,
        functionTypes?: string,
        scriptIds?: string,
        limit?: number,
        cursor?: string,
    ): CancelablePromise<ScriptList> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/data-hub/scripts',
            query: {
                'fields': fields,
                'functionTypes': functionTypes,
                'scriptIds': scriptIds,
                'limit': limit,
                'cursor': cursor,
            },
            errors: {
                503: `Temporary not available`,
            },
        });
    }

    /**
     * Create a new script
     * Creates a script
     * @param requestBody The script that should be created.
     * @returns Script Success
     * @throws ApiError
     */
    public createScript(
        requestBody: Script,
    ): CancelablePromise<Script> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/v1/data-hub/scripts',
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                400: `Script is invalid`,
                409: `Script is already present`,
                412: `Script doesn't match etag`,
                500: `Internal server error`,
                503: `Temporary not available`,
                507: `Insufficient storage`,
            },
        });
    }

    /**
     * Delete a script
     * Deletes the selected script.
     * @param scriptId The script identifier of the script to delete.
     * @returns void
     * @throws ApiError
     */
    public deleteScript(
        scriptId: string,
    ): CancelablePromise<void> {
        return this.httpRequest.request({
            method: 'DELETE',
            url: '/api/v1/data-hub/scripts/{scriptId}',
            path: {
                'scriptId': scriptId,
            },
            errors: {
                400: `Script is referenced`,
                404: `Script not found`,
                412: `Script doesn't match etag`,
                500: `Internal Server error`,
                503: `Temporary not available`,
            },
        });
    }

    /**
     * Get a script
     * Get a specific script.
     * @param scriptId The identifier of the script.
     * @param fields Comma-separated list of fields to include in the response. Allowed values are: id, version, description, runtime, functionType, createdAt
     * @returns Script Success
     * @throws ApiError
     */
    public getScript(
        scriptId: string,
        fields?: string,
    ): CancelablePromise<Script> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/data-hub/scripts/{scriptId}',
            path: {
                'scriptId': scriptId,
            },
            query: {
                'fields': fields,
            },
            errors: {
                400: `URL parameter missing`,
                404: `Script not found`,
                500: `Internal Server error`,
                503: `Temporary not available`,
            },
        });
    }

}
