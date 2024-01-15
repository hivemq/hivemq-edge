/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

/**
 * The client certificate sent by the client
 */
export type CertificateInformation = {
    /**
     * Common name
     */
    commonName?: string | null;
    /**
     * Country
     */
    country?: string | null;
    /**
     * Organization
     */
    organization?: string | null;
    /**
     * Organizational unit
     */
    organizationalUnit?: string | null;
    /**
     * The certificates serial
     */
    serial?: string | null;
    /**
     * State
     */
    state?: string | null;
    /**
     * Valid from date
     */
    validFrom?: string;
    /**
     * Valid until date
     */
    validUntil?: string;
    /**
     * Certificate version
     */
    version?: string | null;
};

