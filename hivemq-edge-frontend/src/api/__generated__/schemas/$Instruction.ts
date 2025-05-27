/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $Instruction = {
    description: `List of instructions to be applied to incoming data`,
    properties: {
        sourceRef: {
            type: 'DataIdentifierReference',
        },
        destination: {
            type: 'string',
            description: `The field in the output object where the data will be written to`,
            isRequired: true,
        },
        source: {
            type: 'string',
            description: `The field in the input object where the data will be read from`,
            isRequired: true,
        },
    },
} as const;
