/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { Bridge } from '../models/Bridge';
import type { BridgeList } from '../models/BridgeList';
import type { Status } from '../models/Status';
import type { StatusList } from '../models/StatusList';
import type { StatusTransitionCommand } from '../models/StatusTransitionCommand';
import type { StatusTransitionResult } from '../models/StatusTransitionResult';

import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';

export class BridgesService {

    constructor(public readonly httpRequest: BaseHttpRequest) {}

    /**
     * List all bridges in the system
     * Get all bridges configured in the system.
     * @returns BridgeList Success
     * @throws ApiError
     */
    public getBridges(): CancelablePromise<BridgeList> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/management/bridges',
        });
    }

    /**
     * Add a new Bridge
     * Add bridge configured in the system.
     * @param requestBody The new bridge.
     * @returns any Success
     * @throws ApiError
     */
    public addBridge(
        requestBody: Bridge,
    ): CancelablePromise<any> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/v1/management/bridges',
            body: requestBody,
            mediaType: 'application/json',
        });
    }

    /**
     * Get the status of all the bridges in the system.
     * Obtain the details.
     * @returns StatusList The Connection Details Verification Result.
     * @throws ApiError
     */
    public getBridgesStatus(): CancelablePromise<StatusList> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/management/bridges/status',
        });
    }

    /**
     * Remove a Bridge
     * Remove bridge configured in the system.
     * @param bridgeId The id of the bridge to delete.
     * @returns any Success
     * @throws ApiError
     */
    public removeBridge(
        bridgeId: string,
    ): CancelablePromise<any> {
        return this.httpRequest.request({
            method: 'DELETE',
            url: '/api/v1/management/bridges/{bridgeId}',
            path: {
                'bridgeId': bridgeId,
            },
        });
    }

    /**
     * Get a bridge by ID
     * Get a bridge by ID.
     * @param bridgeId The id of the bridge to query.
     * @returns Bridge Success
     * @throws ApiError
     */
    public getBridgeByName(
        bridgeId: string,
    ): CancelablePromise<Bridge> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/management/bridges/{bridgeId}',
            path: {
                'bridgeId': bridgeId,
            },
        });
    }

    /**
     * Update a Bridge
     * Update bridge configured in the system.
     * @param bridgeId The bridge to update.
     * @param requestBody
     * @returns any Success
     * @throws ApiError
     */
    public updateBridge(
        bridgeId: string,
        requestBody?: Bridge,
    ): CancelablePromise<any> {
        return this.httpRequest.request({
            method: 'PUT',
            url: '/api/v1/management/bridges/{bridgeId}',
            path: {
                'bridgeId': bridgeId,
            },
            body: requestBody,
            mediaType: 'application/json',
        });
    }

    /**
     * Get the up to date status of a bridge
     * Get the up to date status of a bridge.
     * @param bridgeId The name of the bridge to query.
     * @returns Status Success
     * @throws ApiError
     */
    public getBridgeStatus(
        bridgeId: string,
    ): CancelablePromise<Status> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/management/bridges/{bridgeId}/connection-status',
            path: {
                'bridgeId': bridgeId,
            },
        });
    }

    /**
     * Transition the runtime status of a bridge
     * Transition the connection status of a bridge.
     * @param bridgeId The id of the bridge whose runtime-status will change.
     * @param requestBody The command to transition the bridge runtime status.
     * @returns StatusTransitionResult Success
     * @throws ApiError
     */
    public transitionBridgeStatus(
        bridgeId: string,
        requestBody: StatusTransitionCommand,
    ): CancelablePromise<StatusTransitionResult> {
        return this.httpRequest.request({
            method: 'PUT',
            url: '/api/v1/management/bridges/{bridgeId}/status',
            path: {
                'bridgeId': bridgeId,
            },
            body: requestBody,
            mediaType: 'application/json',
        });
    }

}
