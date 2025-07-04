/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { HealthStatus } from '../models/HealthStatus';

import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';

export class HealthCheckEndpointService {

    constructor(public readonly httpRequest: BaseHttpRequest) {}

    /**
     * Endpoint to determine whether the gateway is considered UP
     * Endpoint to determine whether the gateway is considered UP.
     * @returns HealthStatus Success
     * @throws ApiError
     */
    public liveness(): CancelablePromise<HealthStatus> {
        return this.httpRequest.request({
            method: 'GET',
            url: './api/v1/health/liveness',
        });
    }

    /**
     * Endpoint to determine whether the gateway is considered ready
     * Endpoint to determine whether the gateway is considered ready.
     * @returns HealthStatus Success
     * @throws ApiError
     */
    public readiness(): CancelablePromise<HealthStatus> {
        return this.httpRequest.request({
            method: 'GET',
            url: './api/v1/health/readiness',
        });
    }

}
