/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export const $DataCombining = {
    description: `Define individual rules for data combining, based on the entities selected in the Orchestrator`,
    properties: {
        id: {
            type: 'string',
            description: `The unique id of the data combining mapping`,
            isRequired: true,
            format: 'uuid',
        },
        sources: {
            properties: {
                primary: {
                    type: 'string',
                    isRequired: true,
                },
                primaryType: {
                    type: 'Enum',
                    isRequired: true,
                },
                tags: {
                    type: 'array',
                    contains: {
                        type: 'string',
                    },
                },
                topicFilters: {
                    type: 'array',
                    contains: {
                        type: 'string',
                    },
                },
            },
        },
        destination: {
            type: 'string',
            format: 'mqtt-topic',
        },
        instructions: {
            type: 'array',
            contains: {
                type: 'Instruction',
            },
            isRequired: true,
        },
    },
} as const;
