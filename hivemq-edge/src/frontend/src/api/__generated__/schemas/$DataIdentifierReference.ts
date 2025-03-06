/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $DataIdentifierReference = {
    description: `A reference to one of the data identifiers (topic filter or tag) in Edge`,
    properties: {
        id: {
            type: 'string',
            description: `The name (segmented) of the tag or topic filter`,
            isRequired: true,
        },
        type: {
            type: 'Enum',
            isRequired: true,
        },
    },
} as const;
