/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $ISA95ApiBean = {
    properties: {
        area: {
            type: 'string',
            description: `The area`,
            isNullable: true,
            pattern: '^[a-zA-Z0-9 -_]*$',
        },
        enabled: {
            type: 'boolean',
            description: `Should UNS be available`,
        },
        enterprise: {
            type: 'string',
            description: `The enterprise`,
            isNullable: true,
            pattern: '^[a-zA-Z0-9 -_]*',
        },
        prefixAllTopics: {
            type: 'boolean',
            description: `Should all topics be prefixed with UNS placeholders`,
        },
        productionLine: {
            type: 'string',
            description: `The productionLine`,
            isNullable: true,
            pattern: '^[a-zA-Z0-9 -_]*$',
        },
        site: {
            type: 'string',
            description: `The site`,
            isNullable: true,
            pattern: '^[a-zA-Z0-9 -_]*$',
        },
        workCell: {
            type: 'string',
            description: `The workCell`,
            isNullable: true,
            pattern: '^[a-zA-Z0-9 -_]*$',
        },
    },
} as const;
