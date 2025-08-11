/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $PulseStatus = {
    description: `Information on the activation status of the pulse agent and its connection to the platform.`,
    properties: {
        activation: {
            properties: {
                status: {
                    type: 'Enum',
                    isRequired: true,
                },
                message: {
                    type: 'ProblemDetails',
                },
            },
            isRequired: true,
        },
        runtime: {
            properties: {
                status: {
                    type: 'Enum',
                    isRequired: true,
                },
                message: {
                    type: 'ProblemDetails',
                },
            },
            isRequired: true,
        },
    },
} as const;
