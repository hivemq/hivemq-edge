/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { DiagnosticArchiveItem } from '../models/DiagnosticArchiveItem';

import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';

export class DiagnosticArchiveService {

    constructor(public readonly httpRequest: BaseHttpRequest) {}

    /**
     * Create a new diagnostic archive
     * Triggers the creation of a new diagnostic archive.
     * @returns DiagnosticArchiveItem Success
     * @throws ApiError
     */
    public createDiagnosticArchive(): CancelablePromise<DiagnosticArchiveItem> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/v1/management/diagnostic-archives',
        });
    }

}
