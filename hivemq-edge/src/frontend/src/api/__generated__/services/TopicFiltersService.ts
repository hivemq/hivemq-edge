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
            },
        });
    }

    /**
     * Delete an topic filter
     * Delete the specified topic filter.
     * @param name The topic filter name.
     * @returns any Success
     * @throws ApiError
     */
    public deleteTopicFilter(
        name: string,
    ): CancelablePromise<any> {
        return this.httpRequest.request({
            method: 'DELETE',
            url: '/api/v1/management/topic-filters/{name}',
            path: {
                'name': name,
            },
        });
    }

    /**
     * Update a topic filter.
     * Update a topic filter
     * @param filter The filter of the topic filter that will be updated.
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
            url: '/api/v1/management/topic-filters/{name}',
            path: {
                'filter': filter,
            },
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                403: `Not Found`,
            },
        });
    }

}
