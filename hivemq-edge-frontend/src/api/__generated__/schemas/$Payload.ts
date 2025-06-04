/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $Payload = {
    description: `Object to denote the payload of the event`,
    properties: {
        content: {
            type: 'string',
            description: `The content of the payload encoded as a string`,
        },
        contentType: {
            type: 'Enum',
            isRequired: true,
        },
    },
} as const;
