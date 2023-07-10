/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $DataPoint = {
    properties: {
        sampleTime: {
            type: 'string',
            description: `Time the data-point was generated`,
            isNullable: true,
            format: 'date-time',
        },
        value: {
            type: 'number',
            description: `The value of the data point`,
            format: 'int64',
        },
    },
} as const;
