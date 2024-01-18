import { PanelSpecs, SchemaType } from '@/extensions/datahub/types.ts'

export const MOCK_SCHEMA_SCHEMA: PanelSpecs = {
  schema: {
    type: 'object',
    description: `Data validation relies on the definition of schemas to interact with policies. The HiveMQ Data Hub supports schema definitions with JSON Schema or Protobuf formats:`,
    required: ['type', 'version'],
    properties: {
      type: {
        title: 'Schema',
        enum: [SchemaType.JSON],
        default: SchemaType.JSON,
      },
      version: {
        type: 'string',
      },
      schemaSource: {
        type: 'string',
        format: 'application/schema+json',
      },
    },
  },
  uiSchema: {
    schemaSource: {
      'ui:widget': 'application/schema+json',
    },
    type: {
      'ui:widget': 'radio',
    },
  },
}
