import { PanelSpecs, SchemaType } from '@/extensions/datahub/types.ts'

export const MOCK_SCHEMA_SCHEMA: PanelSpecs = {
  schema: {
    type: 'object',
    description: `Data validation relies on the definition of schemas to interact with policies. The HiveMQ Data Hub supports schema definitions with JSON Schema or Protobuf formats:`,
    required: ['type', 'version'],
    properties: {
      type: {
        title: 'Schema',
        enum: [SchemaType.JSON, SchemaType.PROTO],
        default: SchemaType.PROTO,
      },
      version: {
        type: 'string',
      },
      // schemaSource: {
      //   type: 'string',
      //   format: 'application/schema+json',
      // },
    },
    dependencies: {
      type: {
        oneOf: [
          {
            properties: {
              type: {
                enum: [SchemaType.JSON],
              },
              schemaSource: {
                type: 'string',
                format: 'application/schema+json',
              },
            },
          },
          {
            properties: {
              type: {
                enum: [SchemaType.PROTO],
              },
              schemaSource: {
                type: 'string',
                format: 'application/octet-stream',
              },
            },
          },
        ],
      },
    },
  },
  uiSchema: {
    type: {
      // 'ui:widget': 'radio',
    },
  },
}
