/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $TlsInformation = {
    description: `TLS information`,
    properties: {
        certificateInformation: {
            type: 'CertificateInformation',
        },
        cipherSuite: {
            type: 'string',
            description: `The used cipher suite`,
        },
        tlsVersion: {
            type: 'string',
            description: `The used TLS version`,
        },
    },
    isNullable: true,
} as const;
