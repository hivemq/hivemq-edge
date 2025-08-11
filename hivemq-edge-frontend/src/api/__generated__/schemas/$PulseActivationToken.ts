/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $PulseActivationToken = {
    properties: {
        token: {
            type: 'string',
            description: `The token used to activate the Pulse Client in Edge`,
            isRequired: true,
            format: 'jwt',
        },
    },
} as const;
