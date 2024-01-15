/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $DiagnosticArchive = {
    properties: {
        bytes: {
            type: 'number',
            description: `The size of this diagnostic archive file in bytes.`,
            isNullable: true,
            format: 'int64',
        },
        createdAt: {
            type: 'string',
            description: `Time the diagnostic archive was created at.`,
            format: 'date-time',
        },
        failReason: {
            type: 'string',
            description: `The reason why this diagnostic archive failed, only present for failed diagnostic archives.`,
            isNullable: true,
        },
        id: {
            type: 'string',
            description: `The id of this diagnostic archive.`,
        },
        state: {
            type: 'Enum',
        },
    },
} as const;
