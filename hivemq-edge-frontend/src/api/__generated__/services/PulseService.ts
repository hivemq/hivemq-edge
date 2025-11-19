/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { Combiner } from '../models/Combiner';
import type { CombinerList } from '../models/CombinerList';
import type { DataCombiningList } from '../models/DataCombiningList';
import type { Instruction } from '../models/Instruction';
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
    public addManagedAsset(
        requestBody: ManagedAsset,
    ): CancelablePromise<any> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/v1/management/pulse/managed-assets',
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                400: `Pulse not activated`,
                404: `Managed asset not found`,
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
    public deleteManagedAsset(
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
    public updateManagedAsset(
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

    /**
     * Get all asset mappers
     * Get all asset mappers
     * @returns CombinerList Success
     * @throws ApiError
     */
    public getAssetMappers(): CancelablePromise<CombinerList> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/management/pulse/asset-mappers',
        });
    }

    /**
     * Add a new asset mapper
     * Add a new asset mapper.
     * @param requestBody The combiner to add
     * @returns any Success
     * @throws ApiError
     */
    public addAssetMapper(
        requestBody: Combiner,
    ): CancelablePromise<any> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/v1/management/pulse/asset-mappers',
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                400: `Topic, schema, or mapping ID invalid`,
                404: `Managed asset not found`,
                409: `Combiner already exists`,
            },
        });
    }

    /**
     * Get an asset mapper
     * Get an asset mapper by its unique Id.
     * @param combinerId The unique id of the combiner to retrieve.
     * @returns Combiner Success
     * @throws ApiError
     */
    public getAssetMapper(
        combinerId: string,
    ): CancelablePromise<Combiner> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/management/pulse/asset-mappers/{combinerId}',
            path: {
                'combinerId': combinerId,
            },
            errors: {
                404: `Combiner not found`,
            },
        });
    }

    /**
     * Delete an asset mapper
     * Delete the specified asset mapper.
     * @param combinerId The unique id of the combiner to retrieve.
     * @returns any Success
     * @throws ApiError
     */
    public deleteAssetMapper(
        combinerId: string,
    ): CancelablePromise<any> {
        return this.httpRequest.request({
            method: 'DELETE',
            url: '/api/v1/management/pulse/asset-mappers/{combinerId}',
            path: {
                'combinerId': combinerId,
            },
            errors: {
                404: `Combiner not found`,
            },
        });
    }

    /**
     * Update an asset mapper
     * Update an asset mapper.
     * @param combinerId The unique id of the combiner to retrieve.
     * @param requestBody The new content of the asset mapper
     * @returns any Success
     * @throws ApiError
     */
    public updateAssetMapper(
        combinerId: string,
        requestBody: Combiner,
    ): CancelablePromise<any> {
        return this.httpRequest.request({
            method: 'PUT',
            url: '/api/v1/management/pulse/asset-mappers/{combinerId}',
            path: {
                'combinerId': combinerId,
            },
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                400: `Topic, schema, or mapping ID invalid`,
                404: `Managed asset not found`,
                409: `Combiner already exists`,
            },
        });
    }

    /**
     * Get all mappings
     * Get all data combining mappings for the given combiner
     * @param combinerId The unique id of the combiner to retrieve.
     * @returns DataCombiningList Success
     * @throws ApiError
     */
    public getAssetMapperMappings(
        combinerId: string,
    ): CancelablePromise<DataCombiningList> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/management/pulse/asset-mappers/{combinerId}/mappings',
            path: {
                'combinerId': combinerId,
            },
            errors: {
                404: `Combiner not found`,
            },
        });
    }

    /**
     * Get all instructions
     * Get all the instructions for a designated mapping
     * @param combinerId The unique id of the combiner to retrieve.
     * @param mappingId The unique id of the mapping to retrieve.
     * @returns Instruction Success
     * @throws ApiError
     */
    public getAssetMapperInstructions(
        combinerId: string,
        mappingId: string,
    ): CancelablePromise<Array<Instruction>> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/management/pulse/asset-mappers/{combinerId}/mappings/{mappingId}/instructions',
            path: {
                'combinerId': combinerId,
                'mappingId': mappingId,
            },
            errors: {
                404: `Combiner not found`,
            },
        });
    }

}
