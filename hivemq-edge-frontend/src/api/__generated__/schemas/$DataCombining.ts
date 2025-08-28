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
                    type: 'DataIdentifierReference',
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
            isRequired: true,
        },
        destination: {
            properties: {
                assetId: {
                    type: 'string',
                    description: `The id of a mapped asset containing the topic and schema used for destination of the mapping`,
                    format: 'uuid',
                },
                topic: {
                    type: 'string',
                    description: `The MQTT topic used for destination of the mapping`,
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
