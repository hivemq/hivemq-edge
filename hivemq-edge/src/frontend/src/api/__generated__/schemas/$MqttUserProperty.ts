/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $MqttUserProperty = {
    description: `User properties to be added to each outgoing mqtt message.`,
    properties: {
        name: {
            type: 'string',
            isRequired: true,
        },
        value: {
            type: 'string',
            isRequired: true,
        },
    },
} as const;
