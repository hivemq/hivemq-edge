/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { Adapter } from '../models/Adapter';
import type { AdapterConfigModel } from '../models/AdapterConfigModel';
import type { AdaptersList } from '../models/AdaptersList';
import type { DomainTag } from '../models/DomainTag';
import type { DomainTagList } from '../models/DomainTagList';
import type { FieldMappingsListModel } from '../models/FieldMappingsListModel';
import type { FieldMappingsModel } from '../models/FieldMappingsModel';
import type { JsonNode } from '../models/JsonNode';
import type { ProtocolAdaptersList } from '../models/ProtocolAdaptersList';
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
        requestBody: AdapterConfigModel,
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
            },
        });
    }

    /**
     * Get the field mappings for this adapter.
     * Get the field mappings for this adapter.
     * @param adapterId The adapter id.
     * @returns FieldMappingsListModel Success
     * @throws ApiError
     */
    public getAdapterFieldMappings(
        adapterId: string,
    ): CancelablePromise<FieldMappingsListModel> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/management/protocol-adapters/adapters/{adapterId}/fieldmappings',
            path: {
                'adapterId': adapterId,
            },
        });
    }

    /**
     * Add new field mappings to the specified adapter
     * Add new field mappings to the specified adapter.
     * @param adapterId The adapter id.
     * @param requestBody The field mappings for incoming and outgoing data
     * @returns any Success
     * @throws ApiError
     */
    public addAdapterFieldMappings(
        adapterId: string,
        requestBody: FieldMappingsModel,
    ): CancelablePromise<any> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/v1/management/protocol-adapters/adapters/{adapterId}/fieldmappings',
            path: {
                'adapterId': adapterId,
            },
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                403: `Already Present`,
            },
        });
    }

    /**
     * Update the field mappings of an adapter.
     * Update all field mappings of an adapter.
     * @param adapterId The id of the adapter whose domain tags will be updated.
     * @param requestBody
     * @returns any Success
     * @throws ApiError
     */
    public updateAdapterFieldMappings(
        adapterId: string,
        requestBody?: FieldMappingsListModel,
    ): CancelablePromise<any> {
        return this.httpRequest.request({
            method: 'PUT',
            url: '/api/v1/management/protocol-adapters/adapters/{adapterId}/fieldmappings',
            path: {
                'adapterId': adapterId,
            },
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                403: `Not Found`,
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
                403: `Already Present`,
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
                403: `Not Found`,
            },
        });
    }

    /**
     * Delete an domain tag
     * Delete the specified domain tag on the given adapter.
     * @param adapterId The adapter Id.
     * @param tagId The domain tag Id.
     * @returns any Success
     * @throws ApiError
     */
    public deleteAdapterDomainTags(
        adapterId: string,
        tagId: string,
    ): CancelablePromise<any> {
        return this.httpRequest.request({
            method: 'DELETE',
            url: '/api/v1/management/protocol-adapters/adapters/{adapterId}/tags/{tagId}',
            path: {
                'adapterId': adapterId,
                'tagId': tagId,
            },
        });
    }

    /**
     * Update the domain tag of an adapter.
     * Update the domain tag of an adapter.
     * @param adapterId The id of the adapter whose domain tag will be updated.
     * @param tagId The id of the domain tag that will be changed.
     * @param requestBody
     * @returns any Success
     * @throws ApiError
     */
    public updateAdapterDomainTag(
        adapterId: string,
        tagId: string,
        requestBody?: DomainTag,
    ): CancelablePromise<any> {
        return this.httpRequest.request({
            method: 'PUT',
            url: '/api/v1/management/protocol-adapters/adapters/{adapterId}/tags/{tagId}',
            path: {
                'adapterId': adapterId,
                'tagId': tagId,
            },
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                403: `Not Found`,
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
     * @param tagName The tag name (base64 encoded).
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
            url: '/api/v1/management/protocol-adapters/tagschemas/{protocolId}',
            path: {
                'protocolId': protocolId,
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
        });
    }

    /**
     * Get a json schema that explains the json schema that is used to write to a PLC for the given tag name.
     * Get a json schema that explains the json schema that is used to write to a PLC for the given tag name."
     * @param adapterId The id of the adapter for which the Json Schema for writing to a PLC gets created.
     * @param tagName The tag name (base64 encoded) for which the Json Schema for writing to a PLC gets created.
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
        });
    }

}
