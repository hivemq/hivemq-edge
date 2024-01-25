import { PanelSpecs, StrategyType, ValidatorType } from '@/extensions/datahub/types.ts'

export const MOCK_VALIDATOR_SCHEMA: PanelSpecs = {
  schema: {
    type: 'object',
    definitions: {
      SchemaReference: {
        type: 'object',
        required: ['schemaId', 'version'],
        properties: {
          schemaId: {
            title: 'ID of the schema',
            type: 'string',
          },
          version: {
            title: 'version of the schema',
            type: 'string',
          },
        },
      },
    },
    required: ['type', 'strategy'],
    properties: {
      type: {
        title: 'Validator Type',
        enum: [ValidatorType.SCHEMA],
        default: ValidatorType.SCHEMA,
      },
      strategy: {
        title: 'Validation Strategy',
        enum: [StrategyType.ANY_OF, StrategyType.ALL_OF],
        default: StrategyType.ALL_OF,
      },
      schemas: {
        type: 'array',
        items: {
          $ref: '#/definitions/SchemaReference',
        },
      },
    },
  },
}
