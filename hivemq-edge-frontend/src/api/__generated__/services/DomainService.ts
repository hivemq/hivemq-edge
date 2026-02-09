/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { DomainTagOwnerList } from '../models/DomainTagOwnerList';
import type { ManagedAssetList } from '../models/ManagedAssetList';
import type { NorthboundMappingOwnerList } from '../models/NorthboundMappingOwnerList';
import type { SouthboundMappingOwnerList } from '../models/SouthboundMappingOwnerList';
import type { TopicFilterList } from '../models/TopicFilterList';

import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';

export class DomainService {

    constructor(public readonly httpRequest: BaseHttpRequest) {}

    /**
     * Get all the northbound mappings.
     * Get all northbound mappings
     * @returns NorthboundMappingOwnerList Success
     * @throws ApiError
     */
    public getNorthboundMappings(): CancelablePromise<NorthboundMappingOwnerList> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/management/protocol-adapters/mappings/northboundMappings',
        });
    }

    /**
     * Get all the southbound mappings.
     * Get all southbound mappings.
     * @returns SouthboundMappingOwnerList Success
     * @throws ApiError
     */
    public getSouthboundMappings(): CancelablePromise<SouthboundMappingOwnerList> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/management/protocol-adapters/mappings/southboundMappings',
        });
    }

    /**
     * Get the list of all tags created in this Edge instance
     * Get the list of all domain tags created in this Edge instance
     * @returns DomainTagOwnerList Success
     * @throws ApiError
     */
    public getDomainTags(): CancelablePromise<DomainTagOwnerList> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/management/protocol-adapters/tags',
        });
    }

    /**
     * Get the list of all topic filters created in this Edge instance
     * Get the list of all topic filters created in this Edge instance
     * @returns TopicFilterList Success
     * @throws ApiError
     */
    public getTopicFilters(): CancelablePromise<TopicFilterList> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/management/topic-filters',
        });
    }

    /**
     * Get all managed assets
     * Get all managed assets from the Pulse Client
     * @returns ManagedAssetList Success
     * @throws ApiError
     */
    public getManagedAssets(): CancelablePromise<ManagedAssetList> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/management/pulse/managed-assets',
            errors: {
                400: `Pulse not activated`,
                503: `Pulse Agent not connected`,
            },
        });
    }

}
