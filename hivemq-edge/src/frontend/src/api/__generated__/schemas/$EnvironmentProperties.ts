/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $EnvironmentProperties = {
    description: `A map of properties relating to the installation`,
    properties: {
        properties: {
            type: 'dictionary',
            contains: {
                type: 'string',
                description: `Map of properties that are returned by this endpoint`,
            },
        },
    },
    isNullable: true,
} as const;
