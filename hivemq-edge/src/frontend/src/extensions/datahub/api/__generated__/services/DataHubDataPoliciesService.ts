/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { DataPolicy } from '../models/DataPolicy';
import type { DataPolicyList } from '../models/DataPolicyList';

import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';

export class DataHubDataPoliciesService {

    constructor(public readonly httpRequest: BaseHttpRequest) {}

    /**
     * Get all data policies
     * Get all data policies.
     *
     * This endpoint returns the content of the policies with the content-type `application/json`.
     *
     * This endpoint requires at least HiveMQ version 4.15.0 on all cluster nodes.
     * @param fields Comma-separated list of fields to include in the response. Allowed values are: id, createdAt, lastUpdatedAt, matching, validation, onSuccess, onFailure
     * @param policyIds Comma-separated list of policy IDs used for filtering. Multiple filters can be applied together.
     * @param schemaIds Comma-separated list of schema IDs used for filtering. Multiple filters can be applied together.
     * @param topic MQTT topic string that the retrieved policies must match. Returned policies are sorted in the same way as they are applied to matching publishes. 'topic' filtering does not support pagination
     * @param limit Specifies the page size for the returned results. The value must be between 10 and 500. The default page size is 50. The limit is ignored if the 'topic' query parameter is set.
     * @param cursor The cursor that has been returned by the previous result page. Do not pass this parameter if you want to fetch the first page.
     * @returns DataPolicyList Success
     * @throws ApiError
     */
    public getAllDataPolicies(
        fields?: string,
        policyIds?: string,
        schemaIds?: string,
        topic?: string,
        limit?: number,
        cursor?: string,
    ): CancelablePromise<DataPolicyList> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/data-hub/data-validation/policies',
            query: {
                'fields': fields,
                'policyIds': policyIds,
                'schemaIds': schemaIds,
                'topic': topic,
                'limit': limit,
                'cursor': cursor,
            },
            errors: {
                503: `Not all cluster nodes at minimum version`,
            },
        });
    }

    /**
     * Create a new data policy
     * Create a data policy
     *
     * This endpoint requires at least HiveMQ version 4.15.0 on all cluster nodes.
     * @param requestBody The data policy to create.
     * @returns DataPolicy Success
     * @throws ApiError
     */
    public createDataPolicy(
        requestBody: DataPolicy,
    ): CancelablePromise<DataPolicy> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/v1/data-hub/data-validation/policies',
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                400: `Bad request`,
                503: `Not all cluster nodes at minimum version`,
            },
        });
    }

    /**
     * Delete a data policy
     * Deletes an existing data policy.
     *
     *
     * @param policyId The identifier of the data policy to delete.
     * @returns void
     * @throws ApiError
     */
    public deleteDataPolicy(
        policyId: string,
    ): CancelablePromise<void> {
        return this.httpRequest.request({
            method: 'DELETE',
            url: '/api/v1/data-hub/data-validation/policies/{policyId}',
            path: {
                'policyId': policyId,
            },
            errors: {
                400: `Bad request`,
                404: `Resource not found`,
                503: `Not all cluster nodes at minimum version`,
            },
        });
    }

    /**
     * Get a data policy
     * Get a specific data policy.
     *
     * This endpoint returns the content of the policy with the content-type `application/json`.
     *
     * This endpoint requires at least HiveMQ version 4.15.0 on all cluster nodes.
     * @param policyId The identifier of the policy.
     * @param fields Comma-separated list of fields to include in the response. Allowed values are: id, createdAt, lastUpdatedAt, matching, validation, onSuccess, onFailure
     * @returns DataPolicy Success
     * @throws ApiError
     */
    public getDataPolicy(
        policyId: string,
        fields?: string,
    ): CancelablePromise<DataPolicy> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/data-hub/data-validation/policies/{policyId}',
            path: {
                'policyId': policyId,
            },
            query: {
                'fields': fields,
            },
            errors: {
                400: `Bad request`,
                404: `Resource not found`,
            },
        });
    }

    /**
     * Update an existing data policy
     * Update a data policy
     *
     * The path parameter 'policyId' must match the 'id' of the policy in the request body.
     * The matching part of policies cannot be changed with an update.
     * This endpoint requires at least HiveMQ version 4.17.0 on all cluster nodes.
     * @param policyId The identifier of the policy.
     * @param requestBody The data policy that should be updated.
     * @returns DataPolicy Success
     * @throws ApiError
     */
    public updateDataPolicy(
        policyId: string,
        requestBody: DataPolicy,
    ): CancelablePromise<DataPolicy> {
        return this.httpRequest.request({
            method: 'PUT',
            url: '/api/v1/data-hub/data-validation/policies/{policyId}',
            path: {
                'policyId': policyId,
            },
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                400: `Bad request`,
                404: `Policy not found`,
                412: `Precondition failed`,
                503: `Not all cluster nodes at minimum version`,
            },
        });
    }

}
