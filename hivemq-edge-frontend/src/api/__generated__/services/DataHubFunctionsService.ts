/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { FunctionSpecsList } from '../models/FunctionSpecsList';
import type { JsonNode } from '../models/JsonNode';

import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';

export class DataHubFunctionsService {

    constructor(public readonly httpRequest: BaseHttpRequest) {}

    /**
     * @deprecated
     * Get all functions as a JSON Schema
     * This endpoints provides the means to get information on the available Functions for the HiveMQ Data Hub. The information is provided in form of a Json Schema.
     * @returns JsonNode Success
     * @throws ApiError
     */
    public getFunctions(): CancelablePromise<JsonNode> {
        return this.httpRequest.request({
            method: 'GET',
            url: './api/v1/data-hub/functions',
            errors: {
                500: `Internal server error`,
            },
        });
    }

    /**
     * Get all functions as a list of function specifications
     * This endpoints provides the means to get information on the available Functions for the HiveMQ Data Hub.
     * @returns FunctionSpecsList Success
     * @throws ApiError
     */
    public getFunctionSpecs(): CancelablePromise<FunctionSpecsList> {
        return this.httpRequest.request({
            method: 'GET',
            url: './api/v1/data-hub/function-specs',
            errors: {
                500: `Internal server error`,
            },
        });
    }

}
