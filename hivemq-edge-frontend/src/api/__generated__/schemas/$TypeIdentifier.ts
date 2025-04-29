/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $TypeIdentifier = {
    description: `The unique id of the event object`,
    properties: {
        fullQualifiedIdentifier: {
            type: 'string',
        },
        identifier: {
            type: 'string',
            description: `The identifier associated with the object, a combination of type and identifier is used to uniquely identify an object in the system`,
        },
        type: {
            type: 'Enum',
            isRequired: true,
        },
    },
} as const;
