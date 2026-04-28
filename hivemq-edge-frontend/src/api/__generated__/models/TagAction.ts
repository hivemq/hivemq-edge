/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

/**
 * Action performed on a single tag during import.
 */
export type TagAction = {
    /**
     * The tag name.
     */
    name?: string;
    /**
     * The action performed.
     */
    action?: TagAction.action;
};

export namespace TagAction {

    /**
     * The action performed.
     */
    export enum action {
        CREATED = 'CREATED',
        UPDATED = 'UPDATED',
        DELETED = 'DELETED',
    }


}

