/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

import type { Payload } from './Payload';
import type { TypeIdentifier } from './TypeIdentifier';

/**
 * List of result items that are returned by this endpoint
 */
export type Event = {
    associatedObject?: TypeIdentifier;
    /**
     * Time the event was in date format
     */
    created: string;
    identifier: TypeIdentifier;
    /**
     * The message associated with the event. A message will be no more than 1024 characters in length
     */
    message: string;
    payload?: Payload;
    /**
     * The severity that this log is considered to be
     */
    severity: Event.severity;
    source?: TypeIdentifier;
    /**
     * Time the event was generated in epoch format
     */
    timestamp: number;
};

export namespace Event {

    /**
     * The severity that this log is considered to be
     */
    export enum severity {
        INFO = 'INFO',
        WARN = 'WARN',
        ERROR = 'ERROR',
        CRITICAL = 'CRITICAL',
    }


}

