/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

export type DiagnosticArchive = {
    /**
     * The size of this diagnostic archive file in bytes.
     */
    bytes?: number | null;
    /**
     * Time the diagnostic archive was created at.
     */
    createdAt?: string;
    /**
     * The reason why this diagnostic archive failed, only present for failed diagnostic archives.
     */
    failReason?: string | null;
    /**
     * The id of this diagnostic archive.
     */
    id?: string;
    /**
     * The current state of the diagnostic archive.
     */
    state?: DiagnosticArchive.state;
};

export namespace DiagnosticArchive {

    /**
     * The current state of the diagnostic archive.
     */
    export enum state {
        COMPLETED = 'COMPLETED',
        IN_PROGRESS = 'IN_PROGRESS',
        FAILED = 'FAILED',
    }


}

