/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

export type Backup = {
    /**
     * The size of this backup file in bytes.
     */
    bytes?: number | null;
    /**
     * Time the backup was created at
     */
    createdAt?: string;
    /**
     * The reason why this backup failed, only present for failed backups.
     */
    failReason?: string | null;
    /**
     * The id of this backup
     */
    id?: string;
    /**
     * The current state of the backup
     */
    state?: Backup.state;
};

export namespace Backup {

    /**
     * The current state of the backup
     */
    export enum state {
        COMPLETED = 'COMPLETED',
        RESTORE_COMPLETED = 'RESTORE_COMPLETED',
        IN_PROGRESS = 'IN_PROGRESS',
        RESTORE_IN_PROGRESS = 'RESTORE_IN_PROGRESS',
        FAILED = 'FAILED',
        RESTORE_FAILED = 'RESTORE_FAILED',
    }


}

