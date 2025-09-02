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
        mappingId: {
            type: 'string',
            description: `The id of a DataCombining payload that describes the mapping of that particular asset`,
            format: 'uuid',
        },
    },
} as const;
