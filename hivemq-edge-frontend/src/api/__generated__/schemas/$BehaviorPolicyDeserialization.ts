/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $BehaviorPolicyDeserialization = {
    description: `The deserializers used by the policy for particular message and/or payload types.`,
    properties: {
        publish: {
            type: 'BehaviorPolicyDeserializer',
        },
        will: {
            type: 'BehaviorPolicyDeserializer',
        },
    },
} as const;
