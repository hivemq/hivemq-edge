/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

import type { TagAction } from './TagAction';

/**
 * Result of a bulk device tag import operation.
 */
export type ImportResult = {
    /**
     * Number of new tags created.
     */
    tagsCreated?: number;
    /**
     * Number of existing tags overwritten.
     */
    tagsUpdated?: number;
    /**
     * Number of tags removed.
     */
    tagsDeleted?: number;
    /**
     * Number of northbound mappings created.
     */
    northboundMappingsCreated?: number;
    /**
     * Number of northbound mappings removed.
     */
    northboundMappingsDeleted?: number;
    /**
     * Number of southbound mappings created.
     */
    southboundMappingsCreated?: number;
    /**
     * Number of southbound mappings removed.
     */
    southboundMappingsDeleted?: number;
    /**
     * Per-tag detail of what was done.
     */
    tagActions?: Array<TagAction>;
};

