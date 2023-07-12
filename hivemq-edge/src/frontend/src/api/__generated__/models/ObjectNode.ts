/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

/**
 * List of result items that are returned by this endpoint
 */
export type ObjectNode = {
    children?: Array<ObjectNode>;
    description?: string;
    id?: string;
    name?: string;
    nodeType?: ObjectNode.nodeType;
    selectable?: boolean;
};

export namespace ObjectNode {

    export enum nodeType {
        FOLDER = 'FOLDER',
        OBJECT = 'OBJECT',
        VALUE = 'VALUE',
    }


}

