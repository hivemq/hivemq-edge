/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $FunctionMustBePairedValidationError = {
    type: 'all-of',
    contains: [{
        type: 'ValidationError',
    }, {
        properties: {
            existingFunction: {
                type: 'string',
                description: `The existing function.`,
                isRequired: true,
            },
            missingFunction: {
                type: 'string',
                description: `The missing function.`,
                isRequired: true,
            },
        },
    }],
} as const;
