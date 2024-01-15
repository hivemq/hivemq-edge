/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $TraceRecording = {
    description: `Trace recording item describing the desired (and optionally, when receiving from the server: current) state of a trace recording`,
    properties: {
        clientIdFilters: {
            type: 'array',
            contains: {
                type: 'TraceFilter',
            },
        },
        endAt: {
            type: 'string',
            description: `Time the trace recording is scheduled to stop at. Must be at a later time from the start time`,
            format: 'date-time',
        },
        events: {
            type: 'array',
            contains: {
                type: 'Enum',
            },
        },
        name: {
            type: 'string',
            description: `Name of the trace recording. Must be unique, contain at least three characters and only combinations of numbers, letters, dashes and underscores are allowed`,
        },
        startAt: {
            type: 'string',
            description: `Time the trace recording is scheduled to start at`,
            format: 'date-time',
        },
        state: {
            type: 'Enum',
        },
        topicFilters: {
            type: 'array',
            contains: {
                type: 'TraceFilter',
            },
        },
    },
} as const;
