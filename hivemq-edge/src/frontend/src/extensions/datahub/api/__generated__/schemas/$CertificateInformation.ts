/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $CertificateInformation = {
    description: `The client certificate sent by the client`,
    properties: {
        commonName: {
            type: 'string',
            description: `Common name`,
            isNullable: true,
        },
        country: {
            type: 'string',
            description: `Country`,
            isNullable: true,
        },
        organization: {
            type: 'string',
            description: `Organization`,
            isNullable: true,
        },
        organizationalUnit: {
            type: 'string',
            description: `Organizational unit`,
            isNullable: true,
        },
        serial: {
            type: 'string',
            description: `The certificates serial`,
            isNullable: true,
        },
        state: {
            type: 'string',
            description: `State`,
            isNullable: true,
        },
        validFrom: {
            type: 'string',
            description: `Valid from date`,
            format: 'date-time',
        },
        validUntil: {
            type: 'string',
            description: `Valid until date`,
            format: 'date-time',
        },
        version: {
            type: 'string',
            description: `Certificate version`,
            isNullable: true,
        },
    },
    isNullable: true,
} as const;
