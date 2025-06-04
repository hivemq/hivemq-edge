/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

export type Script = {
    /**
     * The formatted UTC timestamp when the script was created.
     */
    readonly createdAt?: string;
    /**
     * A string of free-form text describing the function.
     */
    description?: string;
    /**
     * The type of the function.
     */
    functionType: Script.functionType;
    /**
     * The unique identifier of the script.
     */
    id: string;
    /**
     * The base64 encoded function source code.
     */
    source: string;
    /**
     * The version of the script.
     */
    readonly version?: number;
};

export namespace Script {

    /**
     * The type of the function.
     */
    export enum functionType {
        TRANSFORMATION = 'TRANSFORMATION',
    }


}

