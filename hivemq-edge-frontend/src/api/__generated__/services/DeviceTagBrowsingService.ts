/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { ImportResult } from '../models/ImportResult';

import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';

export class DeviceTagBrowsingService {

    constructor(public readonly httpRequest: BaseHttpRequest) {}

    /**
     * Browse device tags
     * Browse the device address space of the specified adapter and return discovered nodes as a downloadable file. The response format is determined by the Accept header (text/csv, application/json, or application/yaml).
     * @param adapterId The adapter ID.
     * @param rootId Optional device-specific root identifier to start browsing from.
     * @param maxDepth Max browse depth (0 = unlimited).
     * @returns binary Success
     * @throws ApiError
     */
    public browseDeviceTags(
        adapterId: string,
        rootId?: string,
        maxDepth?: number,
    ): CancelablePromise<Blob> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/v1/management/protocol-adapters/adapters/{adapterId}/device-tags/browse',
            path: {
                'adapterId': adapterId,
            },
            query: {
                'rootId': rootId,
                'maxDepth': maxDepth,
            },
            errors: {
                404: `Adapter not found`,
                409: `Adapter does not support bulk tag browsing or browse failed`,
                504: `Browse timed out`,
            },
        });
    }

    /**
     * Import device tags
     * Import device tags and mappings from a file. The file format is determined by the Content-Type header (text/csv, application/json, or application/yaml). Multiple rows with the same tag name produce multiple northbound mappings for that tag.
     * @param adapterId The adapter ID.
     * @param requestBody The file content (CSV, JSON, or YAML).
     * @param mode Import conflict-resolution mode.
     * @param validateNodes Validate node existence on the device.
     * @returns ImportResult Success
     * @throws ApiError
     */
    public importDeviceTags(
        adapterId: string,
        requestBody: Blob,
        mode: 'CREATE' | 'DELETE' | 'OVERWRITE' | 'MERGE_SAFE' | 'MERGE_OVERWRITE' = 'MERGE_SAFE',
        validateNodes: boolean = false,
    ): CancelablePromise<ImportResult> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/v1/management/protocol-adapters/adapters/{adapterId}/device-tags/import',
            path: {
                'adapterId': adapterId,
            },
            query: {
                'mode': mode,
                'validateNodes': validateNodes,
            },
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                400: `Invalid file or validation errors`,
                404: `Adapter not found`,
                415: `Unsupported media type`,
            },
        });
    }

}
