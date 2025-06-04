/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

/**
 * Object to denote the payload of the event
 */
export type Payload = {
    /**
     * The content of the payload encoded as a string
     */
    content?: string;
    /**
     * The content type of the payload that the event contains
     */
    contentType: Payload.contentType;
};

export namespace Payload {

    /**
     * The content type of the payload that the event contains
     */
    export enum contentType {
        JSON = 'JSON',
        PLAIN_TEXT = 'PLAIN_TEXT',
        XML = 'XML',
        CSV = 'CSV',
    }


}

