/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

/**
 * An associated link
 */
export type Link = {
    /**
     * The optional link display description
     */
    description?: string | null;
    /**
     * The link display text
     */
    displayText?: string | null;
    /**
     * A mandatory Boolean indicating if the link is internal to the context or an external webLink
     */
    external?: boolean;
    /**
     * An optional imageUrl associated with the Link
     */
    imageUrl?: string | null;
    /**
     * An optional target associated with the Link
     */
    target?: string | null;
    /**
     * A mandatory URL associated with the Link
     */
    url: string;
};

