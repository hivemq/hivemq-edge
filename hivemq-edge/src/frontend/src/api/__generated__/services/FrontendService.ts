/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { GatewayConfiguration } from '../models/GatewayConfiguration';
import type { NotificationList } from '../models/NotificationList';

import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';

export class FrontendService {

    constructor(public readonly httpRequest: BaseHttpRequest) {}

    /**
     * Obtain frontend configuration
     * Obtain configuration.
     * @returns GatewayConfiguration Success
     * @throws ApiError
     */
    public getConfiguration(): CancelablePromise<GatewayConfiguration> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/frontend/configuration',
        });
    }

    /**
     * Obtain Notifications
     * Obtain gateway notifications.
     * @returns NotificationList Success
     * @throws ApiError
     */
    public getNotifications(): CancelablePromise<NotificationList> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/frontend/notifications',
        });
    }

}
