/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $DataPolicyValidator = {
    description: `A policy validator which executes the defined validation.`,
    properties: {
        arguments: {
            type: 'dictionary',
            contains: {
                properties: {
                },
            },
            isRequired: true,
        },
        type: {
            type: 'string',
            description: `The type of the validator.`,
            isRequired: true,
        },
    },
} as const;
