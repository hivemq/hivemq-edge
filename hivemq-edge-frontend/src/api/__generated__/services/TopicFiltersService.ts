/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { TopicFilter } from '../models/TopicFilter';
import type { TopicFilterList } from '../models/TopicFilterList';

import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';

export class TopicFiltersService {

    constructor(public readonly httpRequest: BaseHttpRequest) {}

    /**
     * Get the list of all topic filters created in this Edge instance
     * Get the list of all topic filters created in this Edge instance
     * @returns TopicFilterList Success
     * @throws ApiError
     */
    public getTopicFilters(): CancelablePromise<TopicFilterList> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/management/topic-filters',
        });
    }

    /**
     * Add a new topic filter
     * Add a new topic filter.
     * @param requestBody The topic filter.
     * @returns any Success
     * @throws ApiError
     */
    public addTopicFilters(
        requestBody: TopicFilter,
    ): CancelablePromise<any> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/v1/management/topic-filters',
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                403: `Already Present`,
                500: `Internal Server Error`,
            },
        });
    }

    /**
     * Update all topic filters.
     * Update all topic filters
     * @param requestBody
     * @returns any Success
     * @throws ApiError
     */
    public updateTopicFilters(
        requestBody?: TopicFilterList,
    ): CancelablePromise<any> {
        return this.httpRequest.request({
            method: 'PUT',
            url: '/api/v1/management/topic-filters',
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                500: `Internal Server Error`,
            },
        });
    }

    /**
     * Get the specified topic filter
     * Get the specified topic filter
     * @param filter The URL-encoded filter of the topic filter that should be deleted.
     * @returns TopicFilter Success
     * @throws ApiError
     */
    public getTopicFilter(
        filter: string,
    ): CancelablePromise<TopicFilter> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/management/topic-filters/{filter}',
            path: {
                'filter': filter,
            },
        });
    }

    /**
     * Delete an topic filter
     * Delete the specified topic filter.
     * @param filter The URL-encoded filter of the topic filter that should be deleted.
     * @returns any Success
     * @throws ApiError
     */
    public deleteTopicFilter(
        filter: string,
    ): CancelablePromise<any> {
        return this.httpRequest.request({
            method: 'DELETE',
            url: '/api/v1/management/topic-filters/{filter}',
            path: {
                'filter': filter,
            },
            errors: {
                403: `Already Present`,
                404: `Topic filter not found`,
            },
        });
    }

    /**
     * Update a topic filter.
     * Update a topic filter
     * @param filter The URL-encoded filter of the topic filter that should be deleted.
     * @param requestBody
     * @returns any Success
     * @throws ApiError
     */
    public updateTopicFilter(
        filter: string,
        requestBody?: TopicFilter,
    ): CancelablePromise<any> {
        return this.httpRequest.request({
            method: 'PUT',
            url: '/api/v1/management/topic-filters/{filter}',
            path: {
                'filter': filter,
            },
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                400: `Topic filter failed validation`,
                500: `Internal Server Error`,
            },
        });
    }

    /**
     * Get the schema of the specified topic filter
     * Get the schema of the specified topic filter
     * @param filter The URL-encoded filter of the topic filter that should be deleted.
     * @returns string Success
     * @throws ApiError
     */
    public getTopicFilterSchema(
        filter: string,
    ): CancelablePromise<string> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/management/topic-filters/{filter}/schema',
            path: {
                'filter': filter,
            },
        });
    }

}
