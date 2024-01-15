/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

import type { TraceFilter } from './TraceFilter';

/**
 * Trace recording item describing the desired (and optionally, when receiving from the server: current) state of a trace recording
 */
export type TraceRecording = {
    /**
     * Client ID filters to trace
     */
    clientIdFilters?: Array<TraceFilter>;
    /**
     * Time the trace recording is scheduled to stop at. Must be at a later time from the start time
     */
    endAt?: string;
    /**
     * MQTT events to trace
     */
    events?: Array<'MQTT_MESSAGE_CONNECT' | 'MQTT_MESSAGE_CONNACK' | 'MQTT_MESSAGE_SUBSCRIBE' | 'MQTT_MESSAGE_SUBACK' | 'MQTT_MESSAGE_PUBLISH' | 'MQTT_MESSAGE_PUBACK' | 'MQTT_MESSAGE_PUBREC' | 'MQTT_MESSAGE_PUBREL' | 'MQTT_MESSAGE_PUBCOMP' | 'MQTT_MESSAGE_UNSUBSCRIBE' | 'MQTT_MESSAGE_UNSUBACK' | 'MQTT_MESSAGE_PINGREQ' | 'MQTT_MESSAGE_PINGRESP' | 'MQTT_MESSAGE_DISCONNECT' | 'MQTT_MESSAGE_AUTH'>;
    /**
     * Name of the trace recording. Must be unique, contain at least three characters and only combinations of numbers, letters, dashes and underscores are allowed
     */
    name?: string;
    /**
     * Time the trace recording is scheduled to start at
     */
    startAt?: string;
    /**
     * Current state of the recording. Only sent by the API, ignored if specified on POST
     */
    state?: TraceRecording.state;
    /**
     * Topic filters to trace
     */
    topicFilters?: Array<TraceFilter>;
};

export namespace TraceRecording {

    /**
     * Current state of the recording. Only sent by the API, ignored if specified on POST
     */
    export enum state {
        SCHEDULED = 'SCHEDULED',
        IN_PROGRESS = 'IN_PROGRESS',
        ABORTED = 'ABORTED',
        STOPPED = 'STOPPED',
    }


}

