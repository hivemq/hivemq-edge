/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { ClientFilter } from '../models/ClientFilter';
import type { ClientFilterList } from '../models/ClientFilterList';
import type { ClientTopicList } from '../models/ClientTopicList';

import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';

export class ClientService {

    constructor(public readonly httpRequest: BaseHttpRequest) {}

    /**
     * Get a sample of MQTT topics
     * Get a sample of MQTT topics published by clients connected to the Edge broker over a period of time
     * @param queryTime The time limit for the observation.
     * @returns ClientTopicList Success
     * @throws ApiError
     */
    public getClientTopics(
        queryTime: number = 1000,
    ): CancelablePromise<ClientTopicList> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/management/client/topic-samples',
            query: {
                'queryTime': queryTime,
            },
        });
    }

    /**
     * Get a list of MQTT client filters
     * Get a list of all clients filters created for the Edge instance
     * @returns ClientFilterList Success
     * @throws ApiError
     */
    public getClientFilters(): CancelablePromise<ClientFilterList> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/management/client/filters',
        });
    }

    /**
     * Add a client filter
     * Add a client filter to the Edge instance
     * @param requestBody The new client filter
     * @returns any Success
     * @throws ApiError
     */
    public addClientFilter(
        requestBody: ClientFilter,
    ): CancelablePromise<any> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/v1/management/client/filters',
            body: requestBody,
            mediaType: 'application/json',
        });
    }

    /**
     * Get a MQTT client filter
     * Get the details of the specified client filter and its topcis
     * @param clientFilterId The client filter Id.
     * @returns ClientFilter Success
     * @throws ApiError
     */
    public getClientFilter(
        clientFilterId: string,
    ): CancelablePromise<ClientFilter> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/management/client/filters/{clientFilterId}',
            path: {
                'clientFilterId': clientFilterId,
            },
        });
    }

    /**
     * Delete a client filter
     * Delete the specified client filter.
     * @param clientFilterId The client filter Id.
     * @returns any Success
     * @throws ApiError
     */
    public deleteClientFilter(
        clientFilterId: string,
    ): CancelablePromise<any> {
        return this.httpRequest.request({
            method: 'DELETE',
            url: '/api/v1/management/client/filters/{clientFilterId}',
            path: {
                'clientFilterId': clientFilterId,
            },
        });
    }

    /**
     * Update a client filter
     * Update the specified client filter
     * @param clientFilterId The client filter Id.
     * @param requestBody
     * @returns any Success
     * @throws ApiError
     */
    public updateClientFilter(
        clientFilterId: string,
        requestBody?: ClientFilter,
    ): CancelablePromise<any> {
        return this.httpRequest.request({
            method: 'PUT',
            url: '/api/v1/management/client/filters/{clientFilterId}',
            path: {
                'clientFilterId': clientFilterId,
            },
            body: requestBody,
            mediaType: 'application/json',
        });
    }

}
