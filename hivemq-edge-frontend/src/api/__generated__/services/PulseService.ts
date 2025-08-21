/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { ManagedAsset } from '../models/ManagedAsset';
import type { ManagedAssetList } from '../models/ManagedAssetList';
import type { PulseActivationToken } from '../models/PulseActivationToken';
import type { PulseStatus } from '../models/PulseStatus';

import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';

export class PulseService {

    constructor(public readonly httpRequest: BaseHttpRequest) {}

    /**
     * Delete the Pulse activation token
     * Delete the activation token from the Pulse Client
     * @returns any Success
     * @throws ApiError
     */
    public deletePulseActivationToken(): CancelablePromise<any> {
        return this.httpRequest.request({
            method: 'DELETE',
            url: '/api/v1/management/pulse/activation-token',
            errors: {
                409: `Activation token is already deleted.`,
                500: `Internal server error.`,
                503: `Activation token is not deleted successfully.`,
            },
        });
    }

    /**
     * Update an activation token
     * Send an activation token to the Pulse Client
     * @param requestBody The activation token to send to the Pulse Client.
     * @returns any Success
     * @throws ApiError
     */
    public updatePulseActivationToken(
        requestBody: PulseActivationToken,
    ): CancelablePromise<any> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/v1/management/pulse/activation-token',
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                400: `Pulse Agent is deactivated.`,
                422: `Activation token is invalid.`,
                500: `Internal server error.`,
            },
        });
    }

    /**
     * Get the status of the integrated pulse agent.
     * Get the status of the pulse agent
     * @returns PulseStatus Success
     * @throws ApiError
     */
    public getPulseStatus(): CancelablePromise<PulseStatus> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/management/pulse/status',
        });
    }

    /**
     * Get all managed assets
     * Get all managed assets from the Pulse Client
     * @returns ManagedAssetList Success
     * @throws ApiError
     */
    public getManagedAssets(): CancelablePromise<ManagedAssetList> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/management/pulse/managed-assets',
            errors: {
                400: `Pulse not activated`,
                503: `Pulse Agent not connected`,
            },
        });
    }

    /**
     * Add a new managed asset
     * Add a new managed asset to the Pulse Client
     * @param requestBody The managed asset to add
     * @returns any Success
     * @throws ApiError
     */
    public addManagedAssets(
        requestBody: ManagedAsset,
    ): CancelablePromise<any> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/v1/management/pulse/managed-assets',
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                400: `Pulse not activated`,
                409: `The managed asset already exists`,
                503: `Pulse Agent not connected`,
            },
        });
    }

    /**
     * Delete a managed asset
     * Delete the specified managed asset
     * @param assetId The unique id of the managed asset to retrieve.
     * @returns any Success
     * @throws ApiError
     */
    public deleteManagedAssets(
        assetId: string,
    ): CancelablePromise<any> {
        return this.httpRequest.request({
            method: 'DELETE',
            url: '/api/v1/management/pulse/managed-assets/{assetId}',
            path: {
                'assetId': assetId,
            },
            errors: {
                400: `Pulse not activated`,
                404: `Managed asset not found`,
                503: `Pulse Agent not connected`,
            },
        });
    }

    /**
     * Update a managed asset
     * Update a managed asset
     * @param assetId The unique id of the managed asset to retrieve.
     * @param requestBody The new content of the managed asset
     * @returns any Success
     * @throws ApiError
     */
    public updateManagedAssets(
        assetId: string,
        requestBody: ManagedAsset,
    ): CancelablePromise<any> {
        return this.httpRequest.request({
            method: 'PUT',
            url: '/api/v1/management/pulse/managed-assets/{assetId}',
            path: {
                'assetId': assetId,
            },
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                400: `Pulse not activated`,
                404: `Managed asset not found`,
                503: `Pulse Agent not connected`,
            },
        });
    }

}
