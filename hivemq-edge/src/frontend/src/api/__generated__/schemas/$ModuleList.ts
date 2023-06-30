/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $ModuleList = {
    description: `The modules available for installation`,
    properties: {
        items: {
            type: 'array',
            contains: {
                type: 'Module',
            },
        },
    },
} as const;
