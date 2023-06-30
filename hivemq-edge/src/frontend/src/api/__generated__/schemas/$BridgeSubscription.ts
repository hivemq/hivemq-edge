/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $BridgeSubscription = {
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
    },
} as const;
