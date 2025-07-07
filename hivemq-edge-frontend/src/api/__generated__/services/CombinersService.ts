/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { Combiner } from '../models/Combiner';
import type { CombinerList } from '../models/CombinerList';
import type { DataCombiningList } from '../models/DataCombiningList';
import type { Instruction } from '../models/Instruction';

import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';

export class CombinersService {

    constructor(public readonly httpRequest: BaseHttpRequest) {}

    /**
     * Get all combiners
     * Get all combiners
     * @returns CombinerList Success
     * @throws ApiError
     */
    public getCombiners(): CancelablePromise<CombinerList> {
        return this.httpRequest.request({
            method: 'GET',
            url: './api/v1/management/combiners',
        });
    }

    /**
     * Add a new combiner
     * Add a new combiner.
     * @param requestBody The combiner to add
     * @returns any Success
     * @throws ApiError
     */
    public addCombiner(
        requestBody: Combiner,
    ): CancelablePromise<any> {
        return this.httpRequest.request({
            method: 'POST',
            url: './api/v1/management/combiners',
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                409: `Combiner already exists`,
            },
        });
    }

    /**
     * Get a combiner
     * Get a combiner by its unique Id.
     * @param combinerId The unique id of the combiner to retrieve.
     * @returns Combiner Success
     * @throws ApiError
     */
    public getCombinersById(
        combinerId: string,
    ): CancelablePromise<Combiner> {
        return this.httpRequest.request({
            method: 'GET',
            url: './api/v1/management/combiners/{combinerId}',
            path: {
                'combinerId': combinerId,
            },
            errors: {
                404: `Combiner not found`,
            },
        });
    }

    /**
     * Delete a combiner
     * Delete the specified combiner.
     * @param combinerId The unique id of the combiner to retrieve.
     * @returns any Success
     * @throws ApiError
     */
    public deleteCombiner(
        combinerId: string,
    ): CancelablePromise<any> {
        return this.httpRequest.request({
            method: 'DELETE',
            url: './api/v1/management/combiners/{combinerId}',
            path: {
                'combinerId': combinerId,
            },
            errors: {
                404: `Combiner not found`,
            },
        });
    }

    /**
     * Update a combiner
     * Update a combiner.
     * @param combinerId The unique id of the combiner to retrieve.
     * @param requestBody The new content of the combiner
     * @returns any Success
     * @throws ApiError
     */
    public updateCombiner(
        combinerId: string,
        requestBody: Combiner,
    ): CancelablePromise<any> {
        return this.httpRequest.request({
            method: 'PUT',
            url: './api/v1/management/combiners/{combinerId}',
            path: {
                'combinerId': combinerId,
            },
            body: requestBody,
            mediaType: 'application/json',
            errors: {
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
    public getCombinerMappings(
        combinerId: string,
    ): CancelablePromise<DataCombiningList> {
        return this.httpRequest.request({
            method: 'GET',
            url: './api/v1/management/combiners/{combinerId}/mappings',
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
    public getMappingInstructions(
        combinerId: string,
        mappingId: string,
    ): CancelablePromise<Array<Instruction>> {
        return this.httpRequest.request({
            method: 'GET',
            url: './api/v1/management/combiners/{combinerId}/mappings/{mappingId}/instructions',
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
