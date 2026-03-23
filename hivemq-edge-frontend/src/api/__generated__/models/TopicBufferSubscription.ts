/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

/**
 * A topic buffer subscription that buffers MQTT messages for a given topic filter.
 */
export type TopicBufferSubscription = {
    /**
     * The MQTT topic filter for this buffer subscription.
     */
    topicFilter: string;
    /**
     * The maximum number of messages to buffer for this topic filter.
     */
    maxMessages: number;
};

