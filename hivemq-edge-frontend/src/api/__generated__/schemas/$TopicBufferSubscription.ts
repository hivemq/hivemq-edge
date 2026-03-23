/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $TopicBufferSubscription = {
    description: `A topic buffer subscription that buffers MQTT messages for a given topic filter.`,
    properties: {
        topicFilter: {
            type: 'string',
            description: `The MQTT topic filter for this buffer subscription.`,
            isRequired: true,
            format: 'mqtt-topic-filter',
        },
        maxMessages: {
            type: 'number',
            description: `The maximum number of messages to buffer for this topic filter.`,
            isRequired: true,
            format: 'int32',
            minimum: 1,
        },
    },
} as const;
