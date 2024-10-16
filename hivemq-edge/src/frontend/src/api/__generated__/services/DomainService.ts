/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { TagSchema } from '../models/TagSchema';

import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';

export class DomainService {

    constructor(public readonly httpRequest: BaseHttpRequest) {}

    /**
     * Get all domain tags
     * Get the list of all domain tags created in this Edge instance
     * @returns string Success
     * @throws ApiError
     */
    public getDomainTags(): CancelablePromise<Array<string>> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/management/domain/tags',
        });
    }

    /**
     * Get data schemas
     * Get the data schema associated with the specified tags
     * @param tags The list of tags to query the schema from
     * @returns TagSchema Success
     * @throws ApiError
     */
    public getTagSchemas(
        tags: Array<string>,
    ): CancelablePromise<TagSchema> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/management/domain/tags/schema',
            query: {
                'tags': tags,
            },
        });
    }

    /**
     * Get all topic tags
     * Get the list of all topic tags created in this Edge instance
     * @returns string Success
     * @throws ApiError
     */
    public getDomainTopics(): CancelablePromise<Array<string>> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/management/domain/topics',
        });
    }

    /**
     * Get data schemas
     * Get the data schema associated with the specified topics
     * @param topics The list of topics to query the schema from
     * @returns TagSchema Success
     * @throws ApiError
     */
    public getTopicSchemas(
        topics: Array<string>,
    ): CancelablePromise<TagSchema> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/management/domain/topics/schema',
            query: {
                'topics': topics,
            },
        });
    }

}
