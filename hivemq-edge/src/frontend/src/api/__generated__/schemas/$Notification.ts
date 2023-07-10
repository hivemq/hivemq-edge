/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $Notification = {
    description: `List of result items that are returned by this endpoint`,
    properties: {
        description: {
            type: 'string',
            description: `The notification description`,
            isNullable: true,
        },
        level: {
            type: 'Enum',
        },
        link: {
            type: 'Link',
        },
        title: {
            type: 'string',
            description: `The notification title`,
        },
    },
} as const;
