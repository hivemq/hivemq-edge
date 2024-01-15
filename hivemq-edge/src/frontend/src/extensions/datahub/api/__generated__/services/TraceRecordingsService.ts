/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { TraceRecordingItem } from '../models/TraceRecordingItem';
import type { TraceRecordingList } from '../models/TraceRecordingList';

import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';

export class TraceRecordingsService {

    constructor(public readonly httpRequest: BaseHttpRequest) {}

    /**
     * Download a trace recording
     * Download a specific trace recording.
     *
     * This endpoint returns the content of the trace recording with the content-type `application/zip`.
     *
     * Only trace recordings in the states `IN_PROGRESS`, `STOPPED` and `ABORTED` can be downloaded.
     * @param traceRecordingId The id of the trace recording.
     * @returns binary Success
     * @throws ApiError
     */
    public downloadTraceRecordingFile(
        traceRecordingId: string,
    ): CancelablePromise<Blob> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/management/files/trace-recordings/{traceRecordingId}',
            path: {
                'traceRecordingId': traceRecordingId,
            },
            errors: {
                400: `Bad request`,
                404: `Resource not found`,
            },
        });
    }

    /**
     * Get all trace recordings
     * Lists all known trace recordings.
     *
     * Trace recordings can be in different states. These states are:
     * - `SCHEDULED` if the start date for a trace recording is in the future
     * - `STOPPED` if a trace recording has reached its end date or was stopped manually
     * - `IN_PROGRESS` when the trace recording is currently ongoing
     * - `ABORTED` if the trace recording was aborted by the server
     *
     * @returns TraceRecordingList Success
     * @throws ApiError
     */
    public getAllTraceRecordings(): CancelablePromise<TraceRecordingList> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/management/trace-recordings',
        });
    }

    /**
     * Create a trace recording
     * Creates a new trace recording.
     *
     * To create a trace recording you must specify a name, start date, end date, a set of filters and the desired packets that should be traced.
     *
     * At least one client or topic filter and at least one packet is required to create a trace recording.
     *
     * The client and topic filters can be [regular expressions](https://www.hivemq.com/docs/hivemq/4.3/control-center/analytic.html#regular-expressions).
     * @param requestBody The trace recording to create
     * @returns TraceRecordingItem Success
     * @throws ApiError
     */
    public createTraceRecording(
        requestBody?: TraceRecordingItem,
    ): CancelablePromise<TraceRecordingItem> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/v1/management/trace-recordings',
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                400: `Bad request`,
            },
        });
    }

    /**
     * Delete a trace recording
     * Deletes an existing trace recording.
     *
     *
     * @param traceRecordingId The name of the trace recording to delete.
     * @returns void
     * @throws ApiError
     */
    public deleteTraceRecording(
        traceRecordingId: string,
    ): CancelablePromise<void> {
        return this.httpRequest.request({
            method: 'DELETE',
            url: '/api/v1/management/trace-recordings/{traceRecordingId}',
            path: {
                'traceRecordingId': traceRecordingId,
            },
            errors: {
                400: `Bad request`,
                404: `Resource not found`,
            },
        });
    }

    /**
     * Stop a trace recording.
     * Stops an existing trace recording.
     *
     * Only the state of the trace recording can be set to `STOPPED` with this endpoint, changes to other fields are ignored.
     * @param traceRecordingId The name of the trace recording to patch/stop.
     * @param requestBody The trace recording to change
     * @returns TraceRecordingItem Success
     * @throws ApiError
     */
    public stopTraceRecording(
        traceRecordingId: string,
        requestBody?: TraceRecordingItem,
    ): CancelablePromise<TraceRecordingItem> {
        return this.httpRequest.request({
            method: 'PATCH',
            url: '/api/v1/management/trace-recordings/{traceRecordingId}',
            path: {
                'traceRecordingId': traceRecordingId,
            },
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                400: `Bad request`,
                404: `Resource not found`,
            },
        });
    }

}
