/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

import type { TLV } from './TLV';

/**
 * Proxy Protocol information
 */
export type ProxyInformation = {
    /**
     * The client's destination IP as seen by the proxy
     */
    destinationIp?: string;
    /**
     * The client's destination port as seen by the proxy
     */
    destinationPort?: number;
    /**
     * The client's IP as seen by the proxy
     */
    sourceIp?: string;
    /**
     * The client's Port as seen by the proxy
     */
    sourcePort?: number;
    /**
     * Additional TLV fields contained in the proxy protocol information
     */
    tlvs?: Array<TLV> | null;
};

