/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

import type { MqttUserProperty } from './MqttUserProperty';
import type { QoS } from './QoS';

/**
 * List of result items that are returned by this endpoint
 */
export type NorthboundMapping = {
    /**
     * Should tag names be included when sent out.
     */
    includeTagNames: boolean;
    /**
     * Should the timestamp be included when sent out.
     */
    includeTimestamp: boolean;
    maxQoS: QoS;
    /**
     * The message expiry interval.
     */
    messageExpiryInterval: number;
    /**
     * The tag for which values hould be collected and sent out.
     */
    tagName: string;
    /**
     * The target mqtt topic where received tags should be sent to.
     */
    topic: string;
    /**
     * User properties to be added to each outgoing mqtt message.
     */
    userProperties?: Array<MqttUserProperty>;
};

