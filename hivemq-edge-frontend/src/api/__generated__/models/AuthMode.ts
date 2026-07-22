/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export type AuthMode = {
    /**
     * The authentication mode the gateway is configured for.
     */
    mode: AuthMode.mode;
};
export namespace AuthMode {
    /**
     * The authentication mode the gateway is configured for.
     */
    export enum mode {
        LOCAL = 'local',
        OIDC = 'oidc',
    }
}

