/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $LocalBridgeSubscription = {
    description: `localSubscriptions associated with the bridge`,
    properties: {
        customUserProperties: {
            type: 'array',
            contains: {
                type: 'BridgeCustomUserProperty',
            },
        },
        destination: {
            type: 'string',
            description: `The destination topic for this filter set.`,
            isRequired: true,
        },
        excludes: {
            type: 'array',
            contains: {
                type: 'string',
                description: `The exclusion patterns`,
                isNullable: true,
            },
            isNullable: true,
        },
        filters: {
            type: 'array',
            contains: {
                type: 'string',
                description: `The filters for this subscription.`,
            },
            isRequired: true,
        },
        maxQoS: {
            type: 'Enum',
            isRequired: true,
        },
        preserveRetain: {
            type: 'boolean',
            description: `The preserveRetain for this subscription`,
        },
        queueLimit: {
            type: 'number',
            description: `The limit of this bridge for QoS-1 and QoS-2 messages.`,
            isNullable: true,
            format: 'int64',
        },
    },
} as const;
