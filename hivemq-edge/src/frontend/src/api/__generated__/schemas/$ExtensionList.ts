/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $ExtensionList = {
    description: `The extensions available for installation`,
    properties: {
        items: {
            type: 'array',
            contains: {
                type: 'Extension',
            },
            isRequired: true,
        },
    },
} as const;
