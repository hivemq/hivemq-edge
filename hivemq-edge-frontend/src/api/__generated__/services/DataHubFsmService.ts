/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { JsonNode } from '../models/JsonNode';

import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';

export class DataHubFsmService {

    constructor(public readonly httpRequest: BaseHttpRequest) {}

    /**
     * Get all FSMs as a JSON Schema
     * This endpoints provides the means to get information on the available Finite State Machines (FSMs) for Behavior Policies for the HiveMQ Data Hub. The information is provided in form of a Json Schema.
     * @returns JsonNode Success
     * @throws ApiError
     */
    public getFsms(): CancelablePromise<JsonNode> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/data-hub/fsm',
            errors: {
                500: `Internal server error`,
            },
        });
    }

}
