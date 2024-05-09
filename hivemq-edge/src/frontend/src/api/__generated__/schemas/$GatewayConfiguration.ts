/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $GatewayConfiguration = {
    properties: {
        cloudLink: {
            type: 'Link',
        },
        ctas: {
            type: 'LinkList',
        },
        documentationLink: {
            type: 'Link',
        },
        environment: {
            type: 'EnvironmentProperties',
        },
        extensions: {
            type: 'ExtensionList',
        },
        firstUseInformation: {
            type: 'FirstUseInformation',
        },
        gitHubLink: {
            type: 'Link',
        },
        hivemqId: {
            type: 'string',
            description: `The current id of hivemq edge. Changes at restart.`,
        },
        modules: {
            type: 'ModuleList',
        },
        resources: {
            type: 'LinkList',
        },
        trackingAllowed: {
            type: 'boolean',
            description: `Is the tracking of user actions allowed.`,
        },
    },
} as const;
