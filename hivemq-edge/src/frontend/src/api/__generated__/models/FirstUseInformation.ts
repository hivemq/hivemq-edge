/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

/**
 * Information relating to the firstuse experience
 */
export type FirstUseInformation = {
    /**
     * A mandatory Boolean indicating if the gateway is in firstUse mode
     */
    firstUse: boolean;
    /**
     * A description string to use when firstUse = true.
     */
    firstUseDescription?: string | null;
    /**
     * A header string to use when firstUse = true.
     */
    firstUseTitle?: string | null;
    /**
     * A String indicating if the prefill data for the username/password page.
     */
    prefillPassword?: string | null;
    /**
     * A String indicating if the prefill data for the username/password page.
     */
    prefillUsername?: string | null;
};

