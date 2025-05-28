/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $FunctionSpecsList = {
    description: `List of function configurations`,
    properties: {
        items: {
            type: 'array',
            contains: {
                type: 'FunctionSpecs',
            },
            isRequired: true,
        },
    },
} as const;
