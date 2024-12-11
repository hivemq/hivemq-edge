/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { FsmStatesInformationListItem } from '../models/FsmStatesInformationListItem';

import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';

export class DataHubStateService {

    constructor(public readonly httpRequest: BaseHttpRequest) {}

    /**
     * Get the state of a client
     * Use this endpoint to get the stored state of a client for DataHub.
     *
     *
     * @param clientId The client identifier.
     * @returns FsmStatesInformationListItem Success
     * @throws ApiError
     */
    public getClientState(
        clientId: string,
    ): CancelablePromise<FsmStatesInformationListItem> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/data-hub/behavior-validation/states/{clientId}',
            path: {
                'clientId': clientId,
            },
            errors: {
                400: `URL parameter missing`,
                404: `Client is disconnected`,
                500: `Internal Server error`,
            },
        });
    }

}
