/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { Adapter } from '../models/Adapter';
import type { AdaptersList } from '../models/AdaptersList';
import type { ProtocolAdaptersList } from '../models/ProtocolAdaptersList';
import type { Status } from '../models/Status';
import type { StatusList } from '../models/StatusList';
import type { StatusTransitionCommand } from '../models/StatusTransitionCommand';
import type { StatusTransitionResult } from '../models/StatusTransitionResult';
import type { ValuesTree } from '../models/ValuesTree';

import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';

export class ProtocolAdaptersService {

    constructor(public readonly httpRequest: BaseHttpRequest) {}

    /**
     * Obtain a list of configured adapters
     * Obtain a list of configured adapters.
     * @returns AdaptersList Success
     * @throws ApiError
     */
    public getAdapters(): CancelablePromise<AdaptersList> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/management/protocol-adapters/adapters',
        });
    }

    /**
     * Delete an adapter
     * Delete adapter configured in the system.
     * @param adapterId The adapter Id.
     * @returns any Success
     * @throws ApiError
     */
    public deleteAdapter(
        adapterId: string,
    ): CancelablePromise<any> {
        return this.httpRequest.request({
            method: 'DELETE',
            url: '/api/v1/management/protocol-adapters/adapters/{adapterId}',
            path: {
                'adapterId': adapterId,
            },
        });
    }

    /**
     * Obtain the details for a configured adapter for the specified type
     * Obtain the details for a configured adapter for the specified type".
     * @param adapterId The adapter Id.
     * @returns Adapter Success
     * @throws ApiError
     */
    public getAdapter(
        adapterId: string,
    ): CancelablePromise<Adapter> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/management/protocol-adapters/adapters/{adapterId}',
            path: {
                'adapterId': adapterId,
            },
        });
    }

    /**
     * Update an adapter
     * Update adapter configured in the system.
     * @param adapterId The adapter Id.
     * @param requestBody
     * @returns any Success
     * @throws ApiError
     */
    public updateAdapter(
        adapterId: string,
        requestBody?: Adapter,
    ): CancelablePromise<any> {
        return this.httpRequest.request({
            method: 'PUT',
            url: '/api/v1/management/protocol-adapters/adapters/{adapterId}',
            path: {
                'adapterId': adapterId,
            },
            body: requestBody,
            mediaType: 'application/json',
        });
    }

    /**
     * Discover a list of available data points
     * Obtain a list of available values accessible via this protocol adapter.
     * @param adapterId The adapter Id.
     * @param root The root to browse.
     * @param depth The recursive depth to include. Must be larger than 0.
     * @returns ValuesTree Success
     * @throws ApiError
     */
    public discoverDataPoints(
        adapterId: string,
        root?: string,
        depth?: number,
    ): CancelablePromise<ValuesTree> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/management/protocol-adapters/adapters/{adapterId}/discover',
            path: {
                'adapterId': adapterId,
            },
            query: {
                'root': root,
                'depth': depth,
            },
            errors: {
                400: `Protocol adapter does not support discovery`,
            },
        });
    }

    /**
     * Get the up to date status of an adapter
     * Get the up to date status an adapter.
     * @param adapterId The name of the adapter to query.
     * @returns Status Success
     * @throws ApiError
     */
    public getAdapterStatus(
        adapterId: string,
    ): CancelablePromise<Status> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/management/protocol-adapters/adapters/{adapterId}/status',
            path: {
                'adapterId': adapterId,
            },
        });
    }

    /**
     * Transition the runtime status of an adapter
     * Transition the runtime status of an adapter.
     * @param adapterId The id of the adapter whose runtime status will change.
     * @param requestBody The command to transition the adapter runtime status.
     * @returns StatusTransitionResult Success
     * @throws ApiError
     */
    public transitionAdapterStatus(
        adapterId: string,
        requestBody: StatusTransitionCommand,
    ): CancelablePromise<StatusTransitionResult> {
        return this.httpRequest.request({
            method: 'PUT',
            url: '/api/v1/management/protocol-adapters/adapters/{adapterId}/status',
            path: {
                'adapterId': adapterId,
            },
            body: requestBody,
            mediaType: 'application/json',
        });
    }

    /**
     * Add a new Adapter
     * Add adapter to the system.
     * @param adapterType The adapter type.
     * @param requestBody The new adapter.
     * @returns any Success
     * @throws ApiError
     */
    public addAdapter(
        adapterType: string,
        requestBody: Adapter,
    ): CancelablePromise<any> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/v1/management/protocol-adapters/adapters/{adapterType}',
            path: {
                'adapterType': adapterType,
            },
            body: requestBody,
            mediaType: 'application/json',
        });
    }

    /**
     * Get the status of all the adapters in the system.
     * Obtain the details.
     * @returns StatusList The Connection Details Verification Result.
     * @throws ApiError
     */
    public getAdaptersStatus(): CancelablePromise<StatusList> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/management/protocol-adapters/status',
        });
    }

    /**
     * Obtain a list of available protocol adapter types
     * Obtain a list of available protocol adapter types.
     * @returns ProtocolAdaptersList Success
     * @throws ApiError
     */
    public getAdapterTypes(): CancelablePromise<ProtocolAdaptersList> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/management/protocol-adapters/types',
        });
    }

    /**
     * Obtain a list of configured adapters for the specified type
     * Obtain a list of configured adapters for the specified type.
     * @param adapterType The adapter type.
     * @returns AdaptersList Success
     * @throws ApiError
     */
    public getAdaptersForType(
        adapterType: string,
    ): CancelablePromise<AdaptersList> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/management/protocol-adapters/types/{adapterType}',
            path: {
                'adapterType': adapterType,
            },
        });
    }

}
