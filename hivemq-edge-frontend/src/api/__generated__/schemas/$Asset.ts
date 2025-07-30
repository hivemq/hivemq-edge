/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $Asset = {
    description: `The definition of an asset as sourced from the Pulse Broker`,
    properties: {
        id: {
            type: 'string',
            description: `The unique id of the asset`,
            isReadOnly: true,
            isRequired: true,
            format: 'uuid',
        },
        name: {
            type: 'string',
            description: `The user-facing name of the asset`,
            isReadOnly: true,
            isRequired: true,
        },
        topic: {
            type: 'string',
            description: `The topic associated with the asset`,
            isReadOnly: true,
            isRequired: true,
            format: 'mqtt-topic',
        },
        schema: {
            type: 'string',
            description: `The schema associated with the asset, in a JSON Schema and data uri format.`,
            isRequired: true,
            format: 'data-url',
        },
    },
} as const;
