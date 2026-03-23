/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { TopicBufferSubscription } from '../models/TopicBufferSubscription';
import type { TopicBufferSubscriptionList } from '../models/TopicBufferSubscriptionList';

import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';

export class TopicBuffersService {

    constructor(public readonly httpRequest: BaseHttpRequest) {}

    /**
     * Get all topic buffer subscriptions
     * Get the list of all topic buffer subscriptions configured in this Edge instance.
     * @returns TopicBufferSubscriptionList Success
     * @throws ApiError
     */
    public getTopicBufferSubscriptions(): CancelablePromise<TopicBufferSubscriptionList> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/management/topic-buffers',
        });
    }

    /**
     * Add a new topic buffer subscription
     * Add a new topic buffer subscription.
     * @param requestBody The topic buffer subscription to add.
     * @returns any Success
     * @throws ApiError
     */
    public addTopicBufferSubscription(
        requestBody: TopicBufferSubscription,
    ): CancelablePromise<any> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/v1/management/topic-buffers',
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                409: `Already Exists`,
                500: `Internal Server Error`,
            },
        });
    }

    /**
     * Update a topic buffer subscription
     * Update an existing topic buffer subscription identified by its topic filter.
     * @param topicFilter The MQTT topic filter of the subscription to update.
     * @param requestBody
     * @returns any Success
     * @throws ApiError
     */
    public updateTopicBufferSubscription(
        topicFilter: string,
        requestBody: TopicBufferSubscription,
    ): CancelablePromise<any> {
        return this.httpRequest.request({
            method: 'PUT',
            url: '/api/v1/management/topic-buffers',
            query: {
                'topicFilter': topicFilter,
            },
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                404: `Not Found`,
                500: `Internal Server Error`,
            },
        });
    }

    /**
     * Delete a topic buffer subscription
     * Delete the topic buffer subscription identified by its topic filter.
     * @param topicFilter The MQTT topic filter of the subscription to delete.
     * @returns any Success
     * @throws ApiError
     */
    public deleteTopicBufferSubscription(
        topicFilter: string,
    ): CancelablePromise<any> {
        return this.httpRequest.request({
            method: 'DELETE',
            url: '/api/v1/management/topic-buffers',
            query: {
                'topicFilter': topicFilter,
            },
            errors: {
                404: `Not Found`,
            },
        });
    }

}
