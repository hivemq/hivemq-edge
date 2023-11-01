/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

/**
 * The type-identifier of the object who caused the event to be generated
 */
export type TypeIdentifier = {
    fullQualifiedIdentifier?: string;
    /**
     * The identifier associated with the object, a combination of type and identifier is used to uniquely identify an object in the system
     */
    identifier?: string;
    /**
     * The type of the associated object/entity
     */
    type: TypeIdentifier.type;
};

export namespace TypeIdentifier {

    /**
     * The type of the associated object/entity
     */
    export enum type {
        BRIDGE = 'BRIDGE',
        ADAPTER = 'ADAPTER',
        ADAPTER_TYPE = 'ADAPTER_TYPE',
        EVENT = 'EVENT',
        USER = 'USER',
    }


}

