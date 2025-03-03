/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

/**
 * A reference to one of the data identifiers (topic filter or tag) in Edge
 */
export type DataIdentifierReference = {
    /**
     * The name (segmented) of the tag or topic filter
     */
    id: string;
    type: DataIdentifierReference.type;
};

export namespace DataIdentifierReference {

    export enum type {
        TAG = 'TAG',
        TOPIC_FILTER = 'TOPIC_FILTER',
    }


}

