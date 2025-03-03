/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $Combiner = {
    description: `A data combiner, bringing tags (adapters) and topic filters (bridges) together for further northbound data mapping`,
    properties: {
        id: {
            type: 'string',
            description: `The unique id of the data combiner`,
            isRequired: true,
            format: 'uuid',
        },
        name: {
            type: 'string',
            description: `The user-facing name of the combiner`,
            isRequired: true,
        },
        description: {
            type: 'string',
            description: `The user-facing description of the combiner`,
        },
        sources: {
            type: 'EntityReferenceList',
            isRequired: true,
        },
        mappings: {
            type: 'DataCombiningList',
            isRequired: true,
        },
    },
} as const;
