/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

import type { BridgeSubscription } from './BridgeSubscription';
import type { LocalBridgeSubscription } from './LocalBridgeSubscription';
import type { Status } from './Status';
import type { TlsConfiguration } from './TlsConfiguration';

export type Bridge = {
    /**
     * The cleanStart value associated the the MQTT connection.
     */
    cleanStart: boolean;
    /**
     * The client identifier associated the the MQTT connection.
     */
    clientId?: string | null;
    /**
     * The host the bridge connects to - a well formed hostname, ipv4 or ipv6 value.
     */
    host: string;
    /**
     * The bridge id, must be unique and only contain alpha numeric characters with spaces and hyphens.
     */
    id: string;
    /**
     * The keepAlive associated the the MQTT connection.
     */
    keepAlive: number;
    /**
     * localSubscriptions associated with the bridge
     */
    localSubscriptions?: Array<LocalBridgeSubscription>;
    /**
     * Is loop prevention enabled on the connection
     */
    loopPreventionEnabled?: boolean;
    /**
     * Loop prevention hop count
     */
    loopPreventionHopCount?: number;
    /**
     * The password value associated the the MQTT connection.
     */
    password?: string | null;
    /**
     * Shall the publishes for the bridge be persisted.
     */
    persist?: boolean | null;
    /**
     * The port number to connect to
     */
    port: number;
    /**
     * remoteSubscriptions associated with the bridge
     */
    remoteSubscriptions?: Array<BridgeSubscription>;
    /**
     * The sessionExpiry associated the the MQTT connection.
     */
    sessionExpiry: number;
    status?: Status;
    tlsConfiguration?: TlsConfiguration;
    /**
     * The username value associated the the MQTT connection.
     */
    username?: string | null;
};

