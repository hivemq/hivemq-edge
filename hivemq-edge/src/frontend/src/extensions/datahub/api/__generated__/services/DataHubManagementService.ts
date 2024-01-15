/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';

export class DataHubManagementService {

    constructor(public readonly httpRequest: BaseHttpRequest) {}

    /**
     * Start trial mode
     * Use this endpoint to start the Data Hub trial mode.
     *
     * This endpoint requires at least HiveMQ version 4.17.0 on the REST API node.
     * @returns void
     * @throws ApiError
     */
    public startTrialMode(): CancelablePromise<void> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/v1/data-hub/management/start-trial',
            errors: {
                400: `Bad request`,
                503: `Temporarily not available`,
            },
        });
    }

}
