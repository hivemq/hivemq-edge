/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $EntityReference = {
    description: `A reference to one of the main entities in Edge (e.g. device, adapter, edge broker, bridge host)`,
    properties: {
        type: {
            type: 'EntityType',
            isRequired: true,
        },
        id: {
            type: 'string',
            description: `The id of the entity being references in the combiner`,
            isRequired: true,
        },
    },
} as const;
