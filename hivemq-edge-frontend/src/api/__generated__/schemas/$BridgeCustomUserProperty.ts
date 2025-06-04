/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $BridgeCustomUserProperty = {
    description: `The customUserProperties for this subscription`,
    properties: {
        key: {
            type: 'string',
            description: `The key the from the property`,
            isRequired: true,
            format: 'string',
        },
        value: {
            type: 'string',
            description: `The value the from the property`,
            isRequired: true,
            format: 'string',
        },
    },
} as const;
