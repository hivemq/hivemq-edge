/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { Adapter } from '../models/Adapter';
import type { AdapterConfig } from '../models/AdapterConfig';
import type { AdaptersList } from '../models/AdaptersList';
import type { DomainTag } from '../models/DomainTag';
import type { DomainTagList } from '../models/DomainTagList';
import type { JsonNode } from '../models/JsonNode';
import type { NorthboundMappingList } from '../models/NorthboundMappingList';
import type { ProtocolAdaptersList } from '../models/ProtocolAdaptersList';
import type { SouthboundMappingList } from '../models/SouthboundMappingList';
import type { Status } from '../models/Status';
import type { StatusList } from '../models/StatusList';
import type { StatusTransitionCommand } from '../models/StatusTransitionCommand';
import type { StatusTransitionResult } from '../models/StatusTransitionResult';
import type { TagSchema } from '../models/TagSchema';
import type { ValuesTree } from '../models/ValuesTree';

import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';

export class ProtocolAdaptersService {

    constructor(public readonly httpRequest: BaseHttpRequest) {}

    /**
     * Add a new Adapter and all related parts like e.g. tags
     * Add an adapter and all related parts like e.g. tags to the system.
     * @param adaptertype The adapter type.
     * @param adaptername The adapter name.
     * @param requestBody The new adapter.
     * @returns any Success
     * @throws ApiError
     */
    public createCompleteAdapter(
        adaptertype: string,
        adaptername: string,
        requestBody: AdapterConfig,
    ): CancelablePromise<any> {
        return this.httpRequest.request({
            method: 'PUT',
            url: '/api/v1/management/protocol-adapters/adapterconfigs/{adaptertype}/{adaptername}',
            path: {
                'adaptertype': adaptertype,
                'adaptername': adaptername,
            },
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                400: `Adapter failed validation`,
                404: `Adapter type not found`,
                500: `Internal Server Error`,
            },
        });
    }

    /**
     * Obtain a list of configured adapters
     * Obtain a list of configured adapters.
     * @returns AdaptersList Success
     * @throws ApiError
     */
    public getAdapters(): CancelablePromise<AdaptersList> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/management/protocol-adapters/adapters',
        });
    }

    /**
     * Delete an adapter
     * Delete adapter configured in the system.
     * @param adapterId The adapter Id.
     * @returns any Success
     * @throws ApiError
     */
    public deleteAdapter(
        adapterId: string,
    ): CancelablePromise<any> {
        return this.httpRequest.request({
            method: 'DELETE',
            url: '/api/v1/management/protocol-adapters/adapters/{adapterId}',
            path: {
                'adapterId': adapterId,
            },
            errors: {
                404: `Adapter not found`,
            },
        });
    }

    /**
     * Obtain the details for a configured adapter for the specified type
     * Obtain the details for a configured adapter for the specified type".
     * @param adapterId The adapter Id.
     * @returns Adapter Success
     * @throws ApiError
     */
    public getAdapter(
        adapterId: string,
    ): CancelablePromise<Adapter> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/management/protocol-adapters/adapters/{adapterId}',
            path: {
                'adapterId': adapterId,
            },
            errors: {
                404: `Adapter not found`,
            },
        });
    }

    /**
     * Update an adapter
     * Update adapter configured in the system.
     * @param adapterId The adapter Id.
     * @param requestBody
     * @returns any Success
     * @throws ApiError
     */
    public updateAdapter(
        adapterId: string,
        requestBody?: Adapter,
    ): CancelablePromise<any> {
        return this.httpRequest.request({
            method: 'PUT',
            url: '/api/v1/management/protocol-adapters/adapters/{adapterId}',
            path: {
                'adapterId': adapterId,
            },
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                400: `Adapter is invalid`,
                404: `Adapter not found`,
                500: `Internal Server Error`,
            },
        });
    }

    /**
     * Discover a list of available data points
     * Obtain a list of available values accessible via this protocol adapter.
     * @param adapterId The adapter Id.
     * @param root The root to browse.
     * @param depth The recursive depth to include. Must be larger than 0.
     * @returns ValuesTree Success
     * @throws ApiError
     */
    public discoverDataPoints(
        adapterId: string,
        root?: string,
        depth?: number,
    ): CancelablePromise<ValuesTree> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/management/protocol-adapters/adapters/{adapterId}/discover',
            path: {
                'adapterId': adapterId,
            },
            query: {
                'root': root,
                'depth': depth,
            },
            errors: {
                400: `Protocol adapter does not support discovery`,
                404: `Adapter not found`,
                500: `Internal Server Error`,
            },
        });
    }

    /**
     * Get the mappings for northbound messages.
     * Get the northbound mappings of the adapter.
     * @param adapterId The adapter id.
     * @returns NorthboundMappingList Success
     * @throws ApiError
     */
    public getAdapterNorthboundMappings(
        adapterId: string,
    ): CancelablePromise<NorthboundMappingList> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/management/protocol-adapters/adapters/{adapterId}/northboundMappings',
            path: {
                'adapterId': adapterId,
            },
            errors: {
                404: `Adapter not found`,
            },
        });
    }

    /**
     * Update the from mappings of an adapter.
     * Update all northbound mappings of an adapter.
     * @param adapterId The id of the adapter whose northbound mappings will be updated.
     * @param requestBody
     * @returns any Success
     * @throws ApiError
     */
    public updateAdapterNorthboundMappings(
        adapterId: string,
        requestBody?: NorthboundMappingList,
    ): CancelablePromise<any> {
        return this.httpRequest.request({
            method: 'PUT',
            url: '/api/v1/management/protocol-adapters/adapters/{adapterId}/northboundMappings',
            path: {
                'adapterId': adapterId,
            },
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                400: `Missing tags`,
                404: `Adapter not found`,
                500: `Internal Server Error`,
            },
        });
    }

    /**
     * Get the southbound mappings.
     * Get the southbound mappings.
     * @param adapterId The adapter id.
     * @returns SouthboundMappingList Success
     * @throws ApiError
     */
    public getAdapterSouthboundMappings(
        adapterId: string,
    ): CancelablePromise<SouthboundMappingList> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/management/protocol-adapters/adapters/{adapterId}/southboundMappings',
            path: {
                'adapterId': adapterId,
            },
            errors: {
                404: `Adapter not found`,
            },
        });
    }

    /**
     * Update the to southbound mappings of an adapter.
     * Update all southbound mappings of an adapter.
     * @param adapterId The id of the adapter whose southbound mappings will be updated.
     * @param requestBody
     * @returns any Success
     * @throws ApiError
     */
    public updateAdapterSouthboundMappings(
        adapterId: string,
        requestBody?: SouthboundMappingList,
    ): CancelablePromise<any> {
        return this.httpRequest.request({
            method: 'PUT',
            url: '/api/v1/management/protocol-adapters/adapters/{adapterId}/southboundMappings',
            path: {
                'adapterId': adapterId,
            },
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                400: `Missing tags`,
                404: `Adapter not found`,
                500: `Internal Server Error`,
            },
        });
    }

    /**
     * Get the up to date status of an adapter
     * Get the up to date status an adapter.
     * @param adapterId The name of the adapter to query.
     * @returns Status Success
     * @throws ApiError
     */
    public getAdapterStatus(
        adapterId: string,
    ): CancelablePromise<Status> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/management/protocol-adapters/adapters/{adapterId}/status',
            path: {
                'adapterId': adapterId,
            },
            errors: {
                400: `Adapter is invalid`,
                404: `Adapter not found`,
            },
        });
    }

    /**
     * Transition the runtime status of an adapter
     * Transition the runtime status of an adapter.
     * @param adapterId The id of the adapter whose runtime status will change.
     * @param requestBody The command to transition the adapter runtime status.
     * @returns StatusTransitionResult Success
     * @throws ApiError
     */
    public transitionAdapterStatus(
        adapterId: string,
        requestBody: StatusTransitionCommand,
    ): CancelablePromise<StatusTransitionResult> {
        return this.httpRequest.request({
            method: 'PUT',
            url: '/api/v1/management/protocol-adapters/adapters/{adapterId}/status',
            path: {
                'adapterId': adapterId,
            },
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                400: `Adapter is invalid`,
                404: `Adapter not found`,
            },
        });
    }

    /**
     * Get the domain tags for the device connected through this adapter.
     * Get the domain tags for the device connected through this adapter.
     * @param adapterId The adapter id.
     * @returns DomainTagList Success
     * @throws ApiError
     */
    public getAdapterDomainTags(
        adapterId: string,
    ): CancelablePromise<DomainTagList> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/management/protocol-adapters/adapters/{adapterId}/tags',
            path: {
                'adapterId': adapterId,
            },
            errors: {
                404: `Adapter not found`,
            },
        });
    }

    /**
     * Add a new domain tag to the specified adapter
     * Add a new domain tag to the specified adapter.
     * @param adapterId The adapter id.
     * @param requestBody The domain tag.
     * @returns any Success
     * @throws ApiError
     */
    public addAdapterDomainTags(
        adapterId: string,
        requestBody: DomainTag,
    ): CancelablePromise<any> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/v1/management/protocol-adapters/adapters/{adapterId}/tags',
            path: {
                'adapterId': adapterId,
            },
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                404: `Adapter not found`,
                409: `Tag already exists`,
                500: `Internal Server Error`,
            },
        });
    }

    /**
     * Update the domain tag of an adapter.
     * Update all domain tags of an adapter.
     * @param adapterId The id of the adapter whose domain tags will be updated.
     * @param requestBody
     * @returns any Success
     * @throws ApiError
     */
    public updateAdapterDomainTags(
        adapterId: string,
        requestBody?: DomainTagList,
    ): CancelablePromise<any> {
        return this.httpRequest.request({
            method: 'PUT',
            url: '/api/v1/management/protocol-adapters/adapters/{adapterId}/tags',
            path: {
                'adapterId': adapterId,
            },
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                404: `Adapter not found`,
                500: `Internal Server Error`,
            },
        });
    }

    /**
     * Delete an domain tag
     * Delete the specified domain tag on the given adapter.
     * @param adapterId The adapter Id.
     * @param tagName The domain tag Id.
     * @returns any Success
     * @throws ApiError
     */
    public deleteAdapterDomainTags(
        adapterId: string,
        tagName: string,
    ): CancelablePromise<any> {
        return this.httpRequest.request({
            method: 'DELETE',
            url: '/api/v1/management/protocol-adapters/adapters/{adapterId}/tags/{tagName}',
            path: {
                'adapterId': adapterId,
                'tagName': tagName,
            },
            errors: {
                404: `Tag not found`,
                500: `Internal Server Error`,
            },
        });
    }

    /**
     * Update the domain tag of an adapter.
     * Update the domain tag of an adapter.
     * @param adapterId The id of the adapter whose domain tag will be updated.
     * @param tagName The name (urlencoded) of the domain tag that will be changed.
     * @param requestBody
     * @returns any Success
     * @throws ApiError
     */
    public updateAdapterDomainTag(
        adapterId: string,
        tagName: string,
        requestBody?: DomainTag,
    ): CancelablePromise<any> {
        return this.httpRequest.request({
            method: 'PUT',
            url: '/api/v1/management/protocol-adapters/adapters/{adapterId}/tags/{tagName}',
            path: {
                'adapterId': adapterId,
                'tagName': tagName,
            },
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                403: `Adapter not found`,
                404: `Tag not found`,
                500: `Internal Server Error`,
            },
        });
    }

    /**
     * Add a new Adapter
     * Add adapter to the system.
     * @param adapterType The adapter type.
     * @param requestBody The new adapter.
     * @returns any Success
     * @throws ApiError
     */
    public addAdapter(
        adapterType: string,
        requestBody: Adapter,
    ): CancelablePromise<any> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/v1/management/protocol-adapters/adapters/{adapterType}',
            path: {
                'adapterType': adapterType,
            },
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                400: `Adapter is invalid`,
                404: `Adapter type not found`,
            },
        });
    }

    /**
     * Get the mappings for northbound messages.
     * Get all northbound mappings
     * @returns NorthboundMappingList Success
     * @throws ApiError
     */
    public getNorthboundMappings(): CancelablePromise<NorthboundMappingList> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/management/protocol-adapters/northboundMappings',
        });
    }

    /**
     * Get all  southbound mappings.
     * Get all southbound mappings.
     * @returns SouthboundMappingList Success
     * @throws ApiError
     */
    public getSouthboundMappings(): CancelablePromise<SouthboundMappingList> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/management/protocol-adapters/southboundMappings',
        });
    }

    /**
     * Get the status of all the adapters in the system.
     * Obtain the details.
     * @returns StatusList The Connection Details Verification Result.
     * @throws ApiError
     */
    public getAdaptersStatus(): CancelablePromise<StatusList> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/management/protocol-adapters/status',
        });
    }

    /**
     * Obtain the JSON schema for a tag for a specific protocol adapter.
     * Obtain the tag schema for a specific portocol adapter.
     * @param protocolId The protocol id.
     * @returns TagSchema Success
     * @throws ApiError
     */
    public getTagSchema(
        protocolId: string,
    ): CancelablePromise<TagSchema> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/management/protocol-adapters/tag-schemas/{protocolId}',
            path: {
                'protocolId': protocolId,
            },
            errors: {
                404: `Adapter type not found`,
            },
        });
    }

    /**
     * Get the list of all domain tags created in this Edge instance
     * Get the list of all domain tags created in this Edge instance
     * @returns DomainTagList Success
     * @throws ApiError
     */
    public getDomainTags(): CancelablePromise<DomainTagList> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/management/protocol-adapters/tags',
        });
    }

    /**
     * Get the domain tag with the given name in this Edge instance
     * Get a domain tag created in this Edge instance
     * @param tagName The tag name (urlencoded).
     * @returns DomainTag Success
     * @throws ApiError
     */
    public getDomainTag(
        tagName: string,
    ): CancelablePromise<DomainTag> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/management/protocol-adapters/tags/{tagName}',
            path: {
                'tagName': tagName,
            },
            errors: {
                404: `Tag not found`,
            },
        });
    }

    /**
     * Obtain a list of available protocol adapter types
     * Obtain a list of available protocol adapter types.
     * @returns ProtocolAdaptersList Success
     * @throws ApiError
     */
    public getAdapterTypes(): CancelablePromise<ProtocolAdaptersList> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/management/protocol-adapters/types',
        });
    }

    /**
     * Obtain a list of configured adapters for the specified type
     * Obtain a list of configured adapters for the specified type.
     * @param adapterType The adapter type.
     * @returns AdaptersList Success
     * @throws ApiError
     */
    public getAdaptersForType(
        adapterType: string,
    ): CancelablePromise<AdaptersList> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/management/protocol-adapters/types/{adapterType}',
            path: {
                'adapterType': adapterType,
            },
            errors: {
                404: `Adapter type not found`,
            },
        });
    }

    /**
     * Get a json schema that explains the json schema that is used to write to a PLC for the given tag name.
     * Get a json schema that explains the json schema that is used to write to a PLC for the given tag name."
     * @param adapterId The id of the adapter for which the Json Schema for writing to a PLC gets created.
     * @param tagName The tag name (urlencoded) for which the Json Schema for writing to a PLC gets created.
     * @returns JsonNode Success
     * @throws ApiError
     */
    public getWritingSchema(
        adapterId: string,
        tagName: string,
    ): CancelablePromise<JsonNode> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/management/protocol-adapters/writing-schema/{adapterId}/{tagName}',
            path: {
                'adapterId': adapterId,
                'tagName': tagName,
            },
            errors: {
                404: `Adapter not found`,
                500: `Internal Server Error`,
            },
        });
    }

}
