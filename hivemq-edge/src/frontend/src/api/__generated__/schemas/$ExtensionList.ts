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
        },
    },
} as const;
