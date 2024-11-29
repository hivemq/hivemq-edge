/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $FieldMappingModel = {
    properties: {
        destination: {
            type: 'string',
            description: `The field name in the outgoing data`,
        },
        source: {
            type: 'string',
            description: `The field name in the incoming data.`,
        },
        transformation: {
            type: 'TransformationModel',
        },
    },
} as const;
