/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $FieldMapping = {
    description: `Defines how incoming data should be transformed before being sent out.`,
    properties: {
        instructions: {
            type: 'array',
            contains: {
                type: 'Instruction',
            },
            isRequired: true,
        },
        metadata: {
            type: 'Metadata',
            isRequired: true,
        },
    },
} as const;
