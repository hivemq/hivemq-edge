/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $DataPolicyValidation = {
    description: `The section of the policy that defines how incoming MQTT messages are validated. If this section is empty, the result of the policy validation is always successful.`,
    properties: {
        validators: {
            type: 'array',
            contains: {
                type: 'DataPolicyValidator',
            },
        },
    },
} as const;
