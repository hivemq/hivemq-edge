/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $OidcLoginRedirect = {
    description: `The Identity Provider authorization URL the SPA should open to begin the OIDC login.`,
    properties: {
        authorizeUrl: {
            type: 'string',
            description: `The absolute Identity Provider authorization endpoint URL, with the freshly minted state, nonce, and PKCE challenge. The SPA opens this in the login popup.`,
            isRequired: true,
        },
    },
} as const;
