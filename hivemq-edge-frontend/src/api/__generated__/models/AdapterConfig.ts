/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

import type { Adapter } from './Adapter';
import type { DomainTag } from './DomainTag';
import type { NorthboundMapping } from './NorthboundMapping';
import type { SouthboundMapping } from './SouthboundMapping';

export type AdapterConfig = {
    config?: Adapter;
    /**
     * The northbound mappings for this adapter
     */
    northboundMappings?: Array<NorthboundMapping>;
    /**
     * The southbound mappings for this adapter
     */
    southboundMappings?: Array<SouthboundMapping>;
    /**
     * The tags defined for this adapter
     */
    tags?: Array<DomainTag>;
};

