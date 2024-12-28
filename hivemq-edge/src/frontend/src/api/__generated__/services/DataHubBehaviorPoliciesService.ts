/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { BehaviorPolicy } from '../models/BehaviorPolicy';
import type { BehaviorPolicyList } from '../models/BehaviorPolicyList';

import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';

export class DataHubBehaviorPoliciesService {

    constructor(public readonly httpRequest: BaseHttpRequest) {}

    /**
     * Get all policies
     * Get all policies.
     *
     * This endpoint returns the content of the policies with the content-type `application/json`.
     *
     *
     * @param fields Comma-separated list of fields to include in the response. Allowed values are: id, createdAt, lastUpdatedAt, deserialization, matching, behavior, onTransitions
     * @param policyIds Comma-separated list of policy ids used for filtering. Multiple filters can be applied together.
     * @param clientIds Comma-separated list of MQTT client identifiers that are used for filtering. Client identifiers are matched by the retrieved policies. Multiple filters can be applied together.
     * @param limit Specifies the page size for the returned results. Has to be between 10 and 500. Default page size is 50. Limit is ignored if the 'topic' query parameter is set.
     * @param cursor The cursor that has been returned by the previous result page. Do not pass this parameter if you want to fetch the first page.
     * @returns BehaviorPolicyList Success
     * @throws ApiError
     */
    public getAllBehaviorPolicies(
        fields?: string,
        policyIds?: string,
        clientIds?: string,
        limit?: number,
        cursor?: string,
    ): CancelablePromise<BehaviorPolicyList> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/data-hub/behavior-validation/policies',
            query: {
                'fields': fields,
                'policyIds': policyIds,
                'clientIds': clientIds,
                'limit': limit,
                'cursor': cursor,
            },
            errors: {
                503: `Temporarily not available`,
            },
        });
    }

    /**
     * Create a new policy
     * Create a behavior policy
     *
     *
     * @param requestBody The policy that should be created.
     * @returns BehaviorPolicy Success
     * @throws ApiError
     */
    public createBehaviorPolicy(
        requestBody: BehaviorPolicy,
    ): CancelablePromise<BehaviorPolicy> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/v1/data-hub/behavior-validation/policies',
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                400: `Policy creation failed`,
                409: `Already exists`,
                500: `Internal error`,
                503: `Temporarily unavailable`,
                507: `Insufficient storage error`,
            },
        });
    }

    /**
     * Delete a behavior policy
     * Deletes an existing policy.
     *
     *
     * @param policyId The identifier of the policy to delete.
     * @returns void
     * @throws ApiError
     */
    public deleteBehaviorPolicy(
        policyId: string,
    ): CancelablePromise<void> {
        return this.httpRequest.request({
            method: 'DELETE',
            url: '/api/v1/data-hub/behavior-validation/policies/{policyId}',
            path: {
                'policyId': policyId,
            },
            errors: {
                400: `URL parameter missing`,
                404: `Policy not found`,
                412: `Precondition failed`,
                500: `Internal error`,
                503: `Temporarily not available`,
            },
        });
    }

    /**
     * Get a  policy
     * Get a specific policy.
     *
     * This endpoint returns the content of the policy with the content-type `application/json`.
     *
     *
     * @param policyId The identifier of the policy.
     * @param fields Comma-separated list of fields to include in the response. Allowed values are: id, createdAt, lastUpdatedAt, deserialization, matching, behavior, onTransitions
     * @returns BehaviorPolicy Success
     * @throws ApiError
     */
    public getBehaviorPolicy(
        policyId: string,
        fields?: string,
    ): CancelablePromise<BehaviorPolicy> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/data-hub/behavior-validation/policies/{policyId}',
            path: {
                'policyId': policyId,
            },
            query: {
                'fields': fields,
            },
            errors: {
                400: `Invalid query parameter`,
                404: `Policy not found`,
            },
        });
    }

    /**
     * Update an existing policy
     * Update a behavior policy
     *
     * The path parameter 'policyId' must match the 'id' of the policy in the request body.
     *
     * @param policyId The identifier of the policy.
     * @param requestBody The policy that should be updated.
     * @returns BehaviorPolicy Success
     * @throws ApiError
     */
    public updateBehaviorPolicy(
        policyId: string,
        requestBody: BehaviorPolicy,
    ): CancelablePromise<BehaviorPolicy> {
        return this.httpRequest.request({
            method: 'PUT',
            url: '/api/v1/data-hub/behavior-validation/policies/{policyId}',
            path: {
                'policyId': policyId,
            },
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                400: `Policy creation failed`,
                404: `Policy not found`,
                412: `Precondition failed`,
                500: `Internal error`,
                503: `Temporarily unavailable`,
                507: `Insufficient storage error`,
            },
        });
    }

}
