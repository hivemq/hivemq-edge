/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $PolicyOperation = {
    description: `The pipeline to execute when this action is triggered. The operations in the pipeline are executed in order.`,
    properties: {
        arguments: {
            type: 'dictionary',
            contains: {
                properties: {
                },
            },
            isRequired: true,
        },
        functionId: {
            type: 'string',
            description: `The unique id of the referenced function to execute in this operation.`,
            isRequired: true,
        },
        id: {
            type: 'string',
            description: `The unique id of the operation in the pipeline.`,
            isRequired: true,
        },
    },
} as const;
