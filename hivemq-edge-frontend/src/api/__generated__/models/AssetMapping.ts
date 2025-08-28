/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

/**
 * The definition of the mapping for a managed asset in Edge
 */
export type AssetMapping = {
    /**
     * The status of the asset mapping
     */
    status: AssetMapping.status;
    /**
     * The id of a DataCombining payload that describes the mapping of that particular asset
     */
    mappingId: string;
};

export namespace AssetMapping {

    /**
     * The status of the asset mapping
     */
    export enum status {
        UNMAPPED = 'UNMAPPED',
        DRAFT = 'DRAFT',
        STREAMING = 'STREAMING',
        REQUIRES_REMAPPING = 'REQUIRES_REMAPPING',
    }


}

