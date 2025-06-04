/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $TlsConfiguration = {
    description: `tlsConfiguration associated with the bridge`,
    properties: {
        cipherSuites: {
            type: 'array',
            contains: {
                type: 'string',
                description: `The cipherSuites from the config`,
            },
        },
        enabled: {
            type: 'boolean',
            description: `If TLS is used`,
        },
        handshakeTimeout: {
            type: 'number',
            description: `The handshakeTimeout from the config`,
            format: 'int32',
        },
        keystorePassword: {
            type: 'string',
            description: `The keystorePassword from the config`,
        },
        keystorePath: {
            type: 'string',
            description: `The keystorePath from the config`,
            isNullable: true,
        },
        keystoreType: {
            type: 'string',
            description: `The keystoreType from the config`,
        },
        privateKeyPassword: {
            type: 'string',
            description: `The privateKeyPassword from the config`,
        },
        protocols: {
            type: 'array',
            contains: {
                type: 'string',
                description: `The protocols from the config`,
            },
        },
        truststorePassword: {
            type: 'string',
            description: `The truststorePassword from the config`,
        },
        truststorePath: {
            type: 'string',
            description: `The truststorePath from the config`,
            isNullable: true,
        },
        truststoreType: {
            type: 'string',
            description: `The truststoreType from the config`,
        },
        verifyHostname: {
            type: 'boolean',
            description: `The verifyHostname from the config`,
        },
    },
    isNullable: true,
} as const;
