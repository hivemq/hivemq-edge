/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

import type { EntityType } from './EntityType';

/**
 * A reference to one of the main entities in Edge (e.g. device, adapter, edge broker, bridge host)
 */
export type EntityReference = {
    type: EntityType;
    /**
     * The id of the entity being references in the combiner
     */
    id: string;
    /**
     * The source is the primary orchestrator of the combiner
     */
    isPrimary?: boolean;
};

