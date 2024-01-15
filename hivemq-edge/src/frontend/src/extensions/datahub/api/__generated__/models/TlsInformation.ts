/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

import type { CertificateInformation } from './CertificateInformation';

/**
 * TLS information
 */
export type TlsInformation = {
    certificateInformation?: CertificateInformation;
    /**
     * The used cipher suite
     */
    cipherSuite?: string;
    /**
     * The used TLS version
     */
    tlsVersion?: string;
};

