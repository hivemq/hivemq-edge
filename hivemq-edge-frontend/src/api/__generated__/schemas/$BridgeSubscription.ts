/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $BridgeSubscription = {
    description: `remoteSubscriptions associated with the bridge`,
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
    },
} as const;
