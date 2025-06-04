/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $Event = {
    description: `List of result items that are returned by this endpoint`,
    properties: {
        associatedObject: {
            type: 'TypeIdentifier',
        },
        created: {
            type: 'string',
            description: `Time the event was in date format`,
            isRequired: true,
            format: 'date-time',
        },
        identifier: {
            type: 'TypeIdentifier',
            isRequired: true,
        },
        message: {
            type: 'string',
            description: `The message associated with the event. A message will be no more than 1024 characters in length`,
            isRequired: true,
        },
        payload: {
            type: 'Payload',
        },
        severity: {
            type: 'Enum',
            isRequired: true,
        },
        source: {
            type: 'TypeIdentifier',
        },
        timestamp: {
            type: 'number',
            description: `Time the event was generated in epoch format`,
            isRequired: true,
            format: 'int64',
        },
    },
} as const;
