import { type RJSFSchema, UiSchema } from '@rjsf/utils'
import { OutwardMapping } from '@/modules/Mappings/types.ts'
import mqttToXMappings from './mapping.utils.json'

interface MockMapping {
  schema?: RJSFSchema
  uiSchema?: UiSchema
}

/**
 * @deprecated This is a mock, will need to be replaced by OpenAPI specs when available
 */
export const MOCK_OUTWARD_MAPPING_OPCUA: MockMapping = {
  schema: mqttToXMappings as RJSFSchema,
  uiSchema: {
    'ui:submitButtonOptions': {
      // norender: true,
    },
    mqttToOpcuaMappings: {
      // 'ui:field': 'mqtt:transform',
      items: {
        'ui:order': ['*', 'mqttMaxQos'],
        fieldMapping: {
          // This is not working
          'ui:minItems': 1,
        },
      },
    },
  },
}

export const MOCK_MAPPING_DATA: OutwardMapping[] = [
  {
    'mqtt-topic': ['bar/test8', 'pump1/temperature'],
    mapping: [
      {
        source: ['dfdf'],
        destination: 'dfdf',
        transformation: {
          function: 'toString',
          params: 'dffd',
        },
      },
      {
        source: ['dd'],
        destination: 'dffdfd',
        transformation: {
          function: 'toString',
          params: 'fdfgfg',
        },
      },
    ],
    node: 'write/power-management/alert',
  },
  {
    'mqtt-topic': [],
    mapping: [],
    node: '',
  },
  {
    'mqtt-topic': [],
    mapping: [],
    node: '',
  },
]
