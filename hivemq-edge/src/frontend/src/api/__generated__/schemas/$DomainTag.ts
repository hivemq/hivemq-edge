/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $DomainTag = {
    description: `A tag associated with a data point on a device connected to the adapter`,
    properties: {
        tag: {
            type: 'string',
            description: `The Tag associated with the data-point.`,
            isRequired: true,
        },
        dataPoint: {
            type: 'DeviceDataPoint',
            isRequired: true,
        },
    },
} as const;
