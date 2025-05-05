/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { ListenerList } from '../models/ListenerList';

import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';

export class GatewayEndpointService {

    constructor(public readonly httpRequest: BaseHttpRequest) {}

    /**
     * Obtain HiveMQ Edge Configuration
     * Obtain gateway configuration.
     * @returns string Success
     * @throws ApiError
     */
    public getXmlConfiguration(): CancelablePromise<string> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/gateway/configuration',
            errors: {
                405: `Error - function not supported`,
            },
        });
    }

    /**
     * Obtain the listeners configured
     * Obtain listener.
     * @returns ListenerList Success
     * @throws ApiError
     */
    public getListeners(): CancelablePromise<ListenerList> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/gateway/listeners',
        });
    }

}
