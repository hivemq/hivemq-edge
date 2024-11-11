/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { JsonNode } from '../models/JsonNode';
import type { PayloadSampleList } from '../models/PayloadSampleList';

import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';

export class PayloadSamplingService {

    constructor(public readonly httpRequest: BaseHttpRequest) {}

    /**
     * Obtain a JsonSchema based in the stored samples for a given topic.
     * Obtain a JsonSchema based in the stored samples for a given topic.
     * @param topic The topic.
     * @returns JsonNode Success
     * @throws ApiError
     */
    public getSchemaForTopic(
        topic: string,
    ): CancelablePromise<JsonNode> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/management/sampling/schema/{topic}',
            path: {
                'topic': topic,
            },
        });
    }

    /**
     * Obtain a list of samples that their gathered for the given topic.
     * Obtain a list of samples that their gathered for the given topic.
     * @param topic The topic.
     * @returns PayloadSampleList Success
     * @throws ApiError
     */
    public getSamplesForTopic(
        topic: string,
    ): CancelablePromise<PayloadSampleList> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/management/sampling/topic/{topic}',
            path: {
                'topic': topic,
            },
        });
    }

    /**
     * Start sampling for the given topic.
     * Start sampling for the given topic.
     * @param topic The topic.
     * @returns any Success
     * @throws ApiError
     */
    public startSamplingForTopic(
        topic: string,
    ): CancelablePromise<any> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/v1/management/sampling/topic/{topic}',
            path: {
                'topic': topic,
            },
        });
    }

}
