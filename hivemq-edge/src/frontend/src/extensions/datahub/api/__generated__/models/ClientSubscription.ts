/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

/**
 * List of result items that are returned by this endpoint
 */
export type ClientSubscription = {
    /**
     * The No Local flag
     */
    noLocal?: boolean;
    /**
     * The Quality of Service level
     */
    qos?: ClientSubscription.qos;
    /**
     * The Retain As Published flag
     */
    retainAsPublished?: boolean;
    /**
     * Retain handling option
     */
    retainHandling?: ClientSubscription.retainHandling;
    /**
     * The subscription identifier
     */
    subscriptionIdentifier?: number | null;
    /**
     * The MQTT topic filter
     */
    topicFilter?: string;
};

export namespace ClientSubscription {

    /**
     * The Quality of Service level
     */
    export enum qos {
        AT_MOST_ONCE = 'AT_MOST_ONCE',
        AT_LEAST_ONCE = 'AT_LEAST_ONCE',
        EXACTLY_ONCE = 'EXACTLY_ONCE',
    }

    /**
     * Retain handling option
     */
    export enum retainHandling {
        SEND = 'SEND',
        SEND_IF_NEW_SUBSCRIPTION = 'SEND_IF_NEW_SUBSCRIPTION',
        DO_NOT_SEND = 'DO_NOT_SEND',
    }


}

