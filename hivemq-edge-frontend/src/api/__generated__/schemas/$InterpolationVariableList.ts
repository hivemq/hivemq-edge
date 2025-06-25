/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $InterpolationVariableList = {
    description: `The list of interpolation variables that can be used in this Datahub instance`,
    properties: {
        items: {
            type: 'array',
            contains: {
                type: 'InterpolationVariable',
            },
            isRequired: true,
        },
    },
} as const;
