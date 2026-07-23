/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { ApiBearerToken } from '../models/ApiBearerToken';
import type { AuthMode } from '../models/AuthMode';
import type { UsernamePasswordCredentials } from '../models/UsernamePasswordCredentials';
import type { CancelablePromise } from '../core/CancelablePromise';
import type { BaseHttpRequest } from '../core/BaseHttpRequest';
export class AuthenticationService {
    constructor(public readonly httpRequest: BaseHttpRequest) {}
    /**
     * Authorize the presented user to obtain a secure token for use on the API.
     * Authorize the presented user to obtain a secure token for use on the API.
     * @param requestBody
     * @returns ApiBearerToken Username & Password Credentials to Authenticate as.
     * @throws ApiError
     */
    public authenticate(
        requestBody?: UsernamePasswordCredentials,
    ): CancelablePromise<ApiBearerToken> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/v1/auth/authenticate',
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                400: `Error in request.`,
                401: `The requested credentials could not be authenticated.`,
            },
        });
    }
    /**
     * Obtain a fresh JWT for the previously authenticated user.
     * Authorize the presented user to obtain a secure token for use on the API.
     * @returns ApiBearerToken Obtain a new JWT from a previously authentication token.
     * @throws ApiError
     */
    public refreshToken(): CancelablePromise<ApiBearerToken> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/v1/auth/refresh-token',
            errors: {
                401: `The requested credentials could not be authenticated.`,
            },
        });
    }
    /**
     * Authorize the presented user to obtain a secure token for use on the API.
     * Authorize the presented user to obtain a secure token for use on the API.
     * @param requestBody
     * @returns any The token was valid
     * @throws ApiError
     */
    public validateToken(
        requestBody?: ApiBearerToken,
    ): CancelablePromise<any> {
        return this.httpRequest.request({
            method: 'POST',
            url: '/api/v1/auth/validate-token',
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                401: `The token was invalid`,
            },
        });
    }
    /**
     * Report the configured authentication mode.
     * Report which authentication mode the gateway is configured for, so the UI can present the matching login (local username/password form or OIDC single sign-on).
     * @returns AuthMode The configured authentication mode.
     * @throws ApiError
     */
    public authMode(): CancelablePromise<AuthMode> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/auth/mode',
        });
    }
    /**
     * Begin the OIDC login flow.
     * Begin the OIDC authorization-code flow. Redirects the browser to the configured Identity Provider's authorization endpoint with a freshly minted state, nonce, and PKCE challenge.
     * @returns void
     * @throws ApiError
     */
    public oidcLogin(): CancelablePromise<void> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/auth/oidc/login',
            errors: {
                302: `Redirect to the Identity Provider's authorization endpoint.`,
                429: `Too many concurrent logins in flight; retry shortly.`,
                503: `OIDC is not configured or the Identity Provider is unreachable.`,
            },
        });
    }
    /**
     * Complete the OIDC login flow.
     * The OIDC redirect URI. Validates the returned state and ID token, exchanges the authorization code for tokens, maps the IdP roles to Edge roles, and issues a HiveMQ Edge JWT delivered to the SPA.
     * @param code The authorization code returned by the Identity Provider.
     * @param state The opaque state token echoed back by the Identity Provider.
     * @param error An OAuth2 error code, present instead of code/state when the login failed.
     * @param errorDescription A human-readable description of the error.
     * @returns ApiBearerToken Login succeeded; the HiveMQ Edge JWT is delivered to the SPA (by default via an HTML page that posts the token to the opener window).
     * @throws ApiError
     */
    public oidcCallback(
        code?: string,
        state?: string,
        error?: string,
        errorDescription?: string,
    ): CancelablePromise<ApiBearerToken> {
        return this.httpRequest.request({
            method: 'GET',
            url: '/api/v1/auth/oidc/callback',
            query: {
                'code': code,
                'state': state,
                'error': error,
                'error_description': errorDescription,
            },
            errors: {
                401: `The login could not be completed (invalid or expired state, token validation failure, or an IdP error).`,
            },
        });
    }
}
