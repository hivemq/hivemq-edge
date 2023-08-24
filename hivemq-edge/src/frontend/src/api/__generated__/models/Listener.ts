/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

/**
 * List of result items that are returned by this endpoint
 */
export type Listener = {
    /**
     * The extension description
     */
    description?: string | null;
    /**
     * The external hostname
     */
    externalHostname?: string | null;
    /**
     * A mandatory ID hostName with the Listener
     */
    hostName?: string;
    /**
     * The listener name
     */
    name?: string;
    /**
     * The listener port
     */
    port?: number;
    /**
     * A protocol that this listener services
     */
    protocol?: string | null;
    /**
     * The underlying transport that this listener uses
     */
    transport?: Listener.transport | null;
};

export namespace Listener {

    /**
     * The underlying transport that this listener uses
     */
    export enum transport {
        TCP = 'TCP',
        UDP = 'UDP',
        DCCP = 'DCCP',
        SCTP = 'SCTP',
        RSVP = 'RSVP',
        QUIC = 'QUIC',
    }


}

