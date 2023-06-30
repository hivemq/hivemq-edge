/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { GatewayConfiguration } from '../models/GatewayConfiguration';
import type { ListenerList } from '../models/ListenerList';
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
     * Obtain the listeners configured
     * Obtain listener.
     * @returns ListenerList Success
     * @throws ApiError
     */
    public getListeners(): CancelablePromise<ListenerList> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/frontend/listeners',
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

    /**
     * Obtain HiveMQ Edge Configuration
     * Obtain gateway configuration.
     * @returns string Success
     * @throws ApiError
     */
    public getXmlConfiguration(): CancelablePromise<string> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/management/system/configuration',
        });
    }

}
