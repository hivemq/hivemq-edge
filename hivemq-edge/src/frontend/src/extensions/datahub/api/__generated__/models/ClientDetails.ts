/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

import type { ClientRestrictions } from './ClientRestrictions';
import type { ConnectionDetails } from './ConnectionDetails';

export type ClientDetails = {
    /**
     * If this client is connected
     */
    connected?: boolean;
    /**
     * Time the client connection was established
     */
    connectedAt?: string | null;
    connection?: ConnectionDetails;
    /**
     * The MQTT client identifier
     */
    id?: string;
    /**
     * The current message queue size for this client
     */
    messageQueueSize?: number;
    restrictions?: ClientRestrictions;
    /**
     * The session expiry interval
     */
    sessionExpiryInterval?: number | null;
    /**
     * If a will is present for this client
     */
    willPresent?: boolean;
};

