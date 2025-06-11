/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $FunctionSpecs = {
    description: `The configuration of a DataHub operation function`,
    properties: {
        functionId: {
            type: 'string',
            description: `The unique name of the function`,
            isRequired: true,
        },
        metadata: {
            type: 'FunctionMetadata',
            description: `The metadata associated with the function`,
            isRequired: true,
        },
        schema: {
            type: 'JsonNode',
            description: `the full JSON-Schema describimng the function and its arguments`,
            isRequired: true,
        },
        uiSchema: {
            type: 'JsonNode',
            description: `An optional UI Schema to customise the rendering of the configuraton form`,
        },
    },
} as const;
