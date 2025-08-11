/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

import type { DataIdentifierReference } from './DataIdentifierReference';
import type { Instruction } from './Instruction';

/**
 * The definition of the mapping for a managed asset in Edge
 */
export type AssetMapping = {
    /**
     * The status of the asset mapping
     */
    status: AssetMapping.status;
    /**
     * The list of sources used in the asset mapping
     */
    sources: Array<DataIdentifierReference>;
    /**
     * The primary source used for triggering the streaming of the asset. It must be one of the sources.
     */
    primary: DataIdentifierReference;
    /**
     * List of mapping instructions to be applied between the sources and the asset schema
     */
    instructions: Array<Instruction>;
};

export namespace AssetMapping {

    /**
     * The status of the asset mapping
     */
    export enum status {
        UNMAPPED = 'UNMAPPED',
        DRAFT = 'DRAFT',
        STREAMING = 'STREAMING',
    }


}

