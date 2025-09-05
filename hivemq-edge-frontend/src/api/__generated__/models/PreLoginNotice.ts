/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

/**
 * The definition of a notice to be presented to the users before login
 */
export type PreLoginNotice = {
    /**
     * Indicates whether the pre-login notice is enabled or not
     */
    enabled: boolean;
    /**
     * The title of the pre-login notice, also presented to the user
     */
    title: string;
    /**
     * The full text of the pre-login notice
     */
    message: string;
    /**
     * An optional text for a consent checkbox that, if present, users will need to check to continue to the login itself
     */
    consent?: string;
};

