/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { DataPoint } from '../models/DataPoint';
import type { MetricList } from '../models/MetricList';

import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';

export class MetricsEndpointService {

    constructor(public readonly httpRequest: BaseHttpRequest) {}

    /**
     * Obtain a list of available metrics
     * Obtain the latest sample for the metric requested.
     * @returns MetricList Success
     * @throws ApiError
     */
    public getMetrics(): CancelablePromise<MetricList> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/metrics',
        });
    }

    /**
     * Obtain the latest sample for the metric requested
     * Obtain the latest sample for the metric requested.
     * @param metricName The metric to search for.
     * @returns DataPoint Success
     * @throws ApiError
     */
    public getSample(
        metricName: string,
    ): CancelablePromise<DataPoint> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/metrics/{metricName}/latest',
            path: {
                'metricName': metricName,
            },
        });
    }

}
