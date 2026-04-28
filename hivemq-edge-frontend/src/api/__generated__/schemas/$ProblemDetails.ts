/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $ProblemDetails = {
    properties: {
        code: {
            type: 'string',
            description: `Correlation id`,
        },
        detail: {
            type: 'string',
        },
        errors: {
            type: 'array',
            contains: {
                type: 'Error',
            },
        },
        status: {
            type: 'number',
            format: 'int32',
        },
        title: {
            type: 'string',
            isRequired: true,
        },
        type: {
            type: 'string',
            format: 'uri',
        },
    },
} as const;
