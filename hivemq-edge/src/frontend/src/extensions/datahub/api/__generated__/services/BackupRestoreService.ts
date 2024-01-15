/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { BackupItem } from '../models/BackupItem';
import type { BackupList } from '../models/BackupList';

import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';

export class BackupRestoreService {

    constructor(public readonly httpRequest: BaseHttpRequest) {}

    /**
     * List all available backups
     * Lists all available backups with their current state.
     *
     * This endpoint can be used to get an overview over all backups that are in progress or can be restored.
     *
     * Canceled or failed backups are included in the results for up to 1 hour after they have been requested.
     *
     * This endpoint requires at least HiveMQ version 4.4.0. on all cluster nodes.
     * @returns BackupList Success
     * @throws ApiError
     */
    public getAllBackups(): CancelablePromise<BackupList> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/management/backups',
            errors: {
                503: `Temporarily not available`,
            },
        });
    }

    /**
     * Create a new backup
     * Triggers the creation of a new backup.
     *
     * This endpoint requires at least HiveMQ version 4.4.0. on all cluster nodes.
     * @returns BackupItem Success
     * @throws ApiError
     */
    public createBackup(): CancelablePromise<BackupItem> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/v1/management/backups',
            errors: {
                503: `Temporarily not available`,
            },
        });
    }

    /**
     * Get backup information
     * Returns the information for a specific backup with its current state.
     *
     * This endpoint can be used to check the progress of a specific backup when it is being created or being restored.
     *
     * Canceled or failed backups are returned for up to 1 hour after the have been requested.
     *
     * This endpoint requires at least HiveMQ version 4.4.0. on all cluster nodes.
     * @param backupId The id of the backup.
     * @returns BackupItem Success
     * @throws ApiError
     */
    public getBackup(
        backupId: string,
    ): CancelablePromise<BackupItem> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/management/backups/{backupId}',
            path: {
                'backupId': backupId,
            },
            errors: {
                400: `Bad request`,
                404: `Resource not found`,
                503: `Temporarily not available`,
            },
        });
    }

    /**
     * Restore a new backup
     * Triggers the restore of a stored backup.
     *
     * This endpoint requires at least HiveMQ version 4.4.0. on all cluster nodes.
     * @param backupId The id of the backup.
     * @returns BackupItem Success
     * @throws ApiError
     */
    public restoreBackup(
        backupId: string,
    ): CancelablePromise<BackupItem> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/v1/management/backups/{backupId}',
            path: {
                'backupId': backupId,
            },
            errors: {
                400: `Bad request`,
                404: `Resource not found`,
                503: `Temporarily not available`,
            },
        });
    }

    /**
     * Download a backup file
     * Download a specific backup file.
     *
     * This endpoint returns the content of the backup file with the content-type `application/octet-stream`.
     *
     * Only backups in the states `COMPLETED`, `RESTORE_IN_PROGRESS`, `RESTORE_FAILED` or `RESTORE_COMPLETED` can be downloaded.
     *
     * This endpoint requires at least HiveMQ version 4.4.0. on all cluster nodes.
     * @param backupId The id of the backup.
     * @returns binary Success
     * @throws ApiError
     */
    public downloadBackupFile(
        backupId: string,
    ): CancelablePromise<Blob> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/management/files/backups/{backupId}',
            path: {
                'backupId': backupId,
            },
            errors: {
                400: `Bad request`,
                404: `Resource not found`,
                503: `Temporarily not available`,
            },
        });
    }

}
