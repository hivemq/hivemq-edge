/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

import type { ProxyInformation } from './ProxyInformation';
import type { TlsInformation } from './TlsInformation';

/**
 * Information about the clients connection
 */
export type ConnectionDetails = {
    /**
     * Clean start flag
     */
    cleanStart?: boolean;
    /**
     * Id of the HiveMQ listener the client is connected to
     */
    connectedListenerId?: string;
    /**
     * Id of the HiveMQ node the client is connected to
     */
    connectedNodeId?: string;
    /**
     * Connection Keep Alive in seconds
     */
    keepAlive?: number | null;
    /**
     * MQTT version of the client
     */
    mqttVersion?: string;
    /**
     * Password
     */
    password?: string | null;
    proxyInformation?: ProxyInformation;
    /**
     * The client's IP
     */
    sourceIp?: string | null;
    tlsInformation?: TlsInformation;
    /**
     * Username
     */
    username?: string | null;
};

