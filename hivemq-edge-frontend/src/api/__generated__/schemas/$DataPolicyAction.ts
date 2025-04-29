/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $DataPolicyAction = {
    description: `One or more operations the outcome of the validation triggers.  When this field is empty, the outcome of the policy validation does not trigger any operations.`,
    properties: {
        pipeline: {
            type: 'array',
            contains: {
                type: 'PolicyOperation',
            },
        },
    },
} as const;
