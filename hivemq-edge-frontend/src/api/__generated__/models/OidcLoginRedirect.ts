/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
/**
 * The Identity Provider authorization URL the SPA should open to begin the OIDC login.
 */
export type OidcLoginRedirect = {
    /**
     * The absolute Identity Provider authorization endpoint URL, with the freshly minted state, nonce, and PKCE challenge. The SPA opens this in the login popup.
     */
    authorizeUrl: string;
};

