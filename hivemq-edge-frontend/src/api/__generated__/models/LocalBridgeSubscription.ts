/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

import type { BridgeCustomUserProperty } from './BridgeCustomUserProperty';

/**
 * localSubscriptions associated with the bridge
 */
export type LocalBridgeSubscription = {
    /**
     * The customUserProperties for this subscription
     */
    customUserProperties?: Array<BridgeCustomUserProperty>;
    /**
     * The destination topic for this filter set.
     */
    destination: string;
    /**
     * The exclusion patterns
     */
    excludes?: Array<string | null> | null;
    /**
     * The filters for this subscription.
     */
    filters: Array<string>;
    /**
     * The maxQoS for this subscription.
     */
    maxQoS: LocalBridgeSubscription.maxQoS;
    /**
     * The preserveRetain for this subscription
     */
    preserveRetain?: boolean;
    /**
     * The limit of this bridge for QoS-1 and QoS-2 messages.
     */
    queueLimit?: number | null;
};

export namespace LocalBridgeSubscription {

    /**
     * The maxQoS for this subscription.
     */
    export enum maxQoS {
        '_0' = 0,
        '_1' = 1,
        '_2' = 2,
    }


}

