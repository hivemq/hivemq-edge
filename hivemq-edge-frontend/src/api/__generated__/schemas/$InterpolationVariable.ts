/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $InterpolationVariable = {
    properties: {
        variable: {
            type: 'string',
            description: `The unique variable name`,
            isRequired: true,
        },
        type: {
            type: 'Enum',
            isRequired: true,
        },
        description: {
            type: 'string',
            description: `The description of the variable name`,
            isRequired: true,
        },
        policyType: {
            type: 'array',
            contains: {
                type: 'PolicyType',
            },
            isRequired: true,
        },
    },
} as const;
