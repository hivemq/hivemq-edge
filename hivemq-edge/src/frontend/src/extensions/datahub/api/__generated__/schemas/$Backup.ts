/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $Backup = {
    properties: {
        bytes: {
            type: 'number',
            description: `The size of this backup file in bytes.`,
            isNullable: true,
            format: 'int64',
        },
        createdAt: {
            type: 'string',
            description: `Time the backup was created at`,
            format: 'date-time',
        },
        failReason: {
            type: 'string',
            description: `The reason why this backup failed, only present for failed backups.`,
            isNullable: true,
        },
        id: {
            type: 'string',
            description: `The id of this backup`,
        },
        state: {
            type: 'Enum',
        },
    },
} as const;
