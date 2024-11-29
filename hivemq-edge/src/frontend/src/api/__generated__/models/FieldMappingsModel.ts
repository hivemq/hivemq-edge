/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

import type { FieldMappingMetaDataModel } from './FieldMappingMetaDataModel';
import type { FieldMappingModel } from './FieldMappingModel';

/**
 * List of result items that are returned by this endpoint
 */
export type FieldMappingsModel = {
    fieldMapping?: Array<FieldMappingModel>;
    metadata?: FieldMappingMetaDataModel;
    tag?: string;
    /**
     * The topic filter according to the MQTT specification.
     */
    topicFilter?: string;
};

