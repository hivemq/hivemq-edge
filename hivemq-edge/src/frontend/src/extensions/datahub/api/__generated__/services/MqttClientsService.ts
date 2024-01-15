/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { ClientItem } from '../models/ClientItem';
import type { ClientList } from '../models/ClientList';
import type { ClientSubscriptionList } from '../models/ClientSubscriptionList';
import type { ConnectionItem } from '../models/ConnectionItem';

import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';

export class MqttClientsService {

    constructor(public readonly httpRequest: BaseHttpRequest) {}

    /**
     * List all MQTT clients
     * Lists all client sessions (online and offline) known to the whole HiveMQ cluster.
     *
     * The result contains each client's client identifier. For more details about each client you can call the endpoints that have a clientId in their URL.
     *
     * This endpoint uses pagination with a cursor.
     * The results are not sorted in any way, no ordering of any kind is guaranteed.
     *
     * This endpoint requires at least HiveMQ version 4.4.0. on all cluster nodes.
     * @param limit Specifies the page size for the returned results. Has to be between 50 and 2500. Default page size is 500.
     * @param cursor The cursor that has been returned by the previous result page. Do not pass this parameter if you want to fetch the first page.
     * @returns ClientList Success
     * @throws ApiError
     */
    public getAllMqttClients(
        limit?: number,
        cursor?: string,
    ): CancelablePromise<ClientList> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/mqtt/clients',
            query: {
                'limit': limit,
                'cursor': cursor,
            },
            errors: {
                400: `Bad request`,
                410: `Cursor not valid anymore`,
                503: `Not all cluster nodes at minimum version`,
            },
        });
    }

    /**
     * Invalidate a client session
     * Invalidates the client session for a client with the given client identifier. If the client is currently connected, it will be disconnected as well.
     *
     * If your client identifiers contain special characters, please make sure that the clientId is URL encoded (a.k.a. percent-encoding, as in RFC 3986).
     * @param clientId The MQTT client identifier.
     * @param preventWillMessage Whether to prevent the will message.
     * @returns void
     * @throws ApiError
     */
    public invalidateClientSession(
        clientId: string,
        preventWillMessage: boolean = false,
    ): CancelablePromise<void> {
        return this.httpRequest.request({
            method: 'DELETE',
            url: '/api/v1/mqtt/clients/{clientId}',
            path: {
                'clientId': clientId,
            },
            query: {
                'preventWillMessage': preventWillMessage,
            },
            errors: {
                400: `Bad request`,
                404: `Resource not found`,
            },
        });
    }

    /**
     * Get detailed client information
     * Returns detailed information for a specific client with it is current state.
     *
     * Including all session and connection information.
     * If your client identifiers contain special characters, please make sure that the clientId is URL Encoded (a.k.a. percent-encoding, as in RFC 3986).
     * @param clientId The MQTT client identifier.
     * @returns ClientItem Success
     * @throws ApiError
     */
    public getMqttClientDetails(
        clientId: string,
    ): CancelablePromise<ClientItem> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/mqtt/clients/{clientId}',
            path: {
                'clientId': clientId,
            },
            errors: {
                400: `Bad request`,
                404: `Resource not found`,
            },
        });
    }

    /**
     * Disconnect a client
     * Disconnects a specific client if its is currently connected.
     *
     * If your client identifiers contain special characters, please make sure that the clientId is URL Encoded (a.k.a. percent-encoding, as in RFC 3986).
     * @param clientId The MQTT client identifier.
     * @param preventWillMessage Whether to prevent the will message.
     * @returns void
     * @throws ApiError
     */
    public disconnectClient(
        clientId: string,
        preventWillMessage: boolean = false,
    ): CancelablePromise<void> {
        return this.httpRequest.request({
            method: 'DELETE',
            url: '/api/v1/mqtt/clients/{clientId}/connection',
            path: {
                'clientId': clientId,
            },
            query: {
                'preventWillMessage': preventWillMessage,
            },
            errors: {
                400: `Bad request`,
                404: `Resource not found`,
            },
        });
    }

    /**
     * Get a clients connection state
     * Returns the information if a specific client is currently connected.
     *
     * If you are only interested in the connection status of a client prefer this endpoint over the the full client detail.
     * If your client identifiers contain special characters, please make sure that the clientId is URL Encoded (a.k.a. percent-encoding, as in RFC 3986).
     * @param clientId The MQTT client identifier.
     * @returns ConnectionItem Success
     * @throws ApiError
     */
    public getMqttClientConnectionState(
        clientId: string,
    ): CancelablePromise<ConnectionItem> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/mqtt/clients/{clientId}/connection',
            path: {
                'clientId': clientId,
            },
            errors: {
                400: `Bad request`,
                404: `Resource not found`,
            },
        });
    }

    /**
     * List all subscriptions for MQTT client
     * List all subscriptions for a specific client.
     *
     * This endpoint does not support pagination with cursor at the moment, but it might be added in future versions. Please make sure to check if a cursor is returned and another page is available to have a future-proof implementation.
     * @param clientId The MQTT client identifier.
     * @returns ClientSubscriptionList Success
     * @throws ApiError
     */
    public getSubscriptionsForMqttClient(
        clientId: string,
    ): CancelablePromise<ClientSubscriptionList> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/mqtt/clients/{clientId}/subscriptions',
            path: {
                'clientId': clientId,
            },
            errors: {
                400: `Bad request`,
                404: `Resource not found`,
            },
        });
    }

}
