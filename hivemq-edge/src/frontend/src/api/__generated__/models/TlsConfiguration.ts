/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */

/**
 * tlsConfiguration associated with the bridge
 */
export type TlsConfiguration = {
    /**
     * The cipherSuites from the config
     */
    cipherSuites?: Array<string>;
    /**
     * If TLS is used
     */
    enabled?: boolean;
    /**
     * The handshakeTimeout from the config
     */
    handshakeTimeout?: number;
    /**
     * The keystorePassword from the config
     */
    keystorePassword?: string;
    /**
     * The keystorePath from the config
     */
    keystorePath?: string | null;
    /**
     * The keystoreType from the config
     */
    keystoreType?: string;
    /**
     * The privateKeyPassword from the config
     */
    privateKeyPassword?: string;
    /**
     * The protocols from the config
     */
    protocols?: Array<string>;
    /**
     * The truststorePassword from the config
     */
    truststorePassword?: string;
    /**
     * The truststorePath from the config
     */
    truststorePath?: string | null;
    /**
     * The truststoreType from the config
     */
    truststoreType?: string;
    /**
     * The verifyHostname from the config
     */
    verifyHostname?: boolean;
};

