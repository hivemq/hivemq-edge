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
            properties: {
                topic: {
                    type: 'string',
                    format: 'mqtt-topic',
                },
                schema: {
                    type: 'string',
                    description: `The optional json schema for this topic filter in the data uri format.`,
                    format: 'data-url',
                },
            },
            isRequired: true,
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
