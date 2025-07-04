/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { InterpolationVariableList } from '../models/InterpolationVariableList';

import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';

export class DataHubInterpolationService {

    constructor(public readonly httpRequest: BaseHttpRequest) {}

    /**
     * Get all interpolation variables
     * This endpoint provides the means to get information on the interpolation variables available for the HiveMQ Data Hub.
     * @returns InterpolationVariableList Success
     * @throws ApiError
     */
    public getVariables(): CancelablePromise<InterpolationVariableList> {
        return this.httpRequest.request({
            method: 'GET',
            url: './api/v1/data-hub/interpolation-variables',
            errors: {
                500: `Internal server error`,
            },
        });
    }

}
