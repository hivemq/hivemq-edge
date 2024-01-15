/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $BehaviorPolicyOnEvent = {
    description: `One or more operations that are triggered on the event. When this field is empty, the transition does not trigger any operations.`,
    properties: {
        pipeline: {
            type: 'array',
            contains: {
                type: 'PolicyOperation',
            },
            isRequired: true,
        },
    },
} as const;
