/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $Listener = {
    description: `List of result items that are returned by this endpoint`,
    properties: {
        description: {
            type: 'string',
            description: `The extension description`,
            isNullable: true,
        },
        externalHostname: {
            type: 'string',
            description: `The external hostname`,
            isNullable: true,
        },
        hostName: {
            type: 'string',
            description: `A mandatory ID hostName with the Listener`,
        },
        name: {
            type: 'string',
            description: `The listener name`,
        },
        port: {
            type: 'number',
            description: `The listener port`,
            format: 'int32',
        },
        protocol: {
            type: 'string',
            description: `A protocol that this listener services`,
            isNullable: true,
        },
        transport: {
            type: 'Enum',
            isNullable: true,
        },
    },
} as const;
