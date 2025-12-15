/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $ValidationError = {
    properties: {
        detail: {
            type: 'string',
            description: `Detailed contextual description of the validation error.`,
            isRequired: true,
        },
        type: {
            type: 'string',
            description: `Type of the validation error.`,
            isRequired: true,
            format: 'uri',
        },
    },
} as const;
