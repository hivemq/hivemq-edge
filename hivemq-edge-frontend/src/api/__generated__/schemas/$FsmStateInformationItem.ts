/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $FsmStateInformationItem = {
    description: `List of result items that are returned by this endpoint`,
    properties: {
        arguments: {
            type: 'JsonNode',
        },
        behaviorId: {
            type: 'string',
            description: `The unique identifier of the policy.`,
        },
        firstSetAt: {
            type: 'string',
            description: `The timestamp when this state was set the first time.`,
        },
        policyId: {
            type: 'string',
            description: `The unique identifier of the policy.`,
        },
        stateName: {
            type: 'string',
            description: `The name of the fsm state.`,
        },
        stateType: {
            type: 'string',
            description: `The type of the fsm state.`,
        },
        variables: {
            type: 'dictionary',
            contains: {
                type: 'string',
                description: `The variables for this fsm.`,
            },
        },
    },
} as const;
