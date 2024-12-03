/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

import type { MqttUserProperty } from './MqttUserProperty';

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
    /**
     * The maximum MQTT-QoS for the outgoing messages.
     */
    maxQoS: number;
    /**
     * The message expiry interval.
     */
    messageExpiryInterval: number;
    /**
     * How collected tags should or shouldnÖT be aggregated.
     */
    messageHandlingOptions: NorthboundMapping.messageHandlingOptions;
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

export namespace NorthboundMapping {

    /**
     * How collected tags should or shouldnÖT be aggregated.
     */
    export enum messageHandlingOptions {
        MQTTMESSAGE_PER_TAG = 'MQTTMessagePerTag',
        MQTTMESSAGE_PER_SUBSCRIPTION = 'MQTTMessagePerSubscription',
    }


}

