/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $ProxyInformation = {
    description: `Proxy Protocol information`,
    properties: {
        destinationIp: {
            type: 'string',
            description: `The client's destination IP as seen by the proxy`,
        },
        destinationPort: {
            type: 'number',
            description: `The client's destination port as seen by the proxy`,
            format: 'int32',
        },
        sourceIp: {
            type: 'string',
            description: `The client's IP as seen by the proxy`,
        },
        sourcePort: {
            type: 'number',
            description: `The client's Port as seen by the proxy`,
            format: 'int32',
        },
        tlvs: {
            type: 'array',
            contains: {
                type: 'TLV',
            },
            isNullable: true,
        },
    },
    isNullable: true,
} as const;
