/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

/**
 * List of result items that are returned by this endpoint
 */
export type Capability = {
    /**
     * A description for the capability
     */
    description?: string;
    /**
     * A human readable name, intended to be used to display at front end.
     */
    displayName?: string;
    /**
     * The identifier of this capability
     */
    id?: Capability.id;
};

export namespace Capability {

    /**
     * The identifier of this capability
     */
    export enum id {
        CONFIG_WRITEABLE = 'config-writeable',
        BI_DIRECTIONAL_PROTOCOL_ADAPTERS = 'bi-directional protocol adapters',
        CONTROL_PLANE_CONNECTIVITY = 'control-plane-connectivity',
        DATA_HUB = 'data-hub',
        MQTT_PERSISTENCE = 'mqtt-persistence',
    }


}

