/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $IllegalEventTransitionValidationError = {
    type: 'all-of',
    contains: [{
        type: 'ValidationError',
    }, {
        properties: {
            event: {
                type: 'string',
                description: `The event name.`,
                isRequired: true,
            },
            fromState: {
                type: 'string',
                description: `The event from state.`,
                isRequired: true,
            },
            id: {
                type: 'string',
                description: `The event id.`,
                isRequired: true,
            },
            path: {
                type: 'string',
                description: `The path.`,
                isRequired: true,
            },
            toState: {
                type: 'string',
                description: `The event to state.`,
                isRequired: true,
            },
        },
    }],
} as const;
