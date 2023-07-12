/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { ISA95ApiBean } from '../models/ISA95ApiBean';

import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';

export class UnsService {

    constructor(public readonly httpRequest: BaseHttpRequest) {}

    /**
     * Obtain isa95 config
     * Obtain isa95 config.
     * @returns ISA95ApiBean Success
     * @throws ApiError
     */
    public getIsa95(): CancelablePromise<ISA95ApiBean> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/management/uns/isa95',
        });
    }

    /**
     * Set isa95 config
     * Set isa95 config.
     * @param requestBody The updated isa95 configuration.
     * @returns any Success
     * @throws ApiError
     */
    public setIsa95(
        requestBody: ISA95ApiBean,
    ): CancelablePromise<any> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/v1/management/uns/isa95',
            body: requestBody,
            mediaType: 'application/json',
        });
    }

}
