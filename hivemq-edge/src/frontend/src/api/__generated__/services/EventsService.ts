/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { EventList } from '../models/EventList';

import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';

export class EventsService {

    constructor(public readonly httpRequest: BaseHttpRequest) {}

    /**
     * List most recent events in the system
     * Get all bridges configured in the system.
     * @param limit Obtain all events since the specified epoch.
     * @param since Obtain all events since the specified epoch.
     * @returns EventList Success
     * @throws ApiError
     */
    public getEvents(
        limit: number = 100,
        since?: number,
    ): CancelablePromise<EventList> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/management/events',
            query: {
                'limit': limit,
                'since': since,
            },
        });
    }

}
