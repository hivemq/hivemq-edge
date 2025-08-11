/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $AssetMapping = {
    description: `The definition of the mapping for a managed asset in Edge`,
    properties: {
        status: {
            type: 'Enum',
            isRequired: true,
        },
        sources: {
            type: 'array',
            contains: {
                type: 'DataIdentifierReference',
            },
            isRequired: true,
        },
        primary: {
            type: 'DataIdentifierReference',
            description: `The primary source used for triggering the streaming of the asset. It must be one of the sources.`,
            isRequired: true,
        },
        instructions: {
            type: 'array',
            contains: {
                type: 'Instruction',
            },
            isRequired: true,
        },
    },
} as const;
