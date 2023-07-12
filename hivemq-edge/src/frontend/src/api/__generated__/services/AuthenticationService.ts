/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { ApiBearerToken } from '../models/ApiBearerToken';
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

}
