/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $Error = {
    properties: {
        detail: {
            type: 'string',
            description: `Detailed contextual description of this error`,
            isRequired: true,
        },
        parameter: {
            type: 'string',
            description: `The parameter causing the issue`,
        },
    },
} as const;
