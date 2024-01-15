/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $ClientSubscription = {
    description: `List of result items that are returned by this endpoint`,
    properties: {
        noLocal: {
            type: 'boolean',
            description: `The No Local flag`,
        },
        qos: {
            type: 'Enum',
        },
        retainAsPublished: {
            type: 'boolean',
            description: `The Retain As Published flag`,
        },
        retainHandling: {
            type: 'Enum',
        },
        subscriptionIdentifier: {
            type: 'number',
            description: `The subscription identifier`,
            isNullable: true,
            format: 'int32',
        },
        topicFilter: {
            type: 'string',
            description: `The MQTT topic filter`,
        },
    },
} as const;
