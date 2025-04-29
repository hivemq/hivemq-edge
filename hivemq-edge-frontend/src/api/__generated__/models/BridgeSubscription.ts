/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

import type { BridgeCustomUserProperty } from './BridgeCustomUserProperty';

/**
 * remoteSubscriptions associated with the bridge
 */
export type BridgeSubscription = {
    /**
     * The customUserProperties for this subscription
     */
    customUserProperties?: Array<BridgeCustomUserProperty>;
    /**
     * The destination topic for this filter set.
     */
    destination: string;
    /**
     * The filters for this subscription.
     */
    filters: Array<string>;
    /**
     * The maxQoS for this subscription.
     */
    maxQoS: BridgeSubscription.maxQoS;
    /**
     * The preserveRetain for this subscription
     */
    preserveRetain?: boolean;
};

export namespace BridgeSubscription {

    /**
     * The maxQoS for this subscription.
     */
    export enum maxQoS {
        '_0' = 0,
        '_1' = 1,
        '_2' = 2,
    }


}

