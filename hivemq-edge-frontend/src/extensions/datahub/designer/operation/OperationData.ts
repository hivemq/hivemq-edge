/* istanbul ignore file -- @preserve */
import type { RJSFSchema } from '@rjsf/utils'

import { CustomFormat } from '@/api/types/json-schema.ts'
import { registerEntitySelectWidget } from '@/components/rjsf/Widgets/EntitySelectWidget.tsx'

import schema from '@datahub/api/__generated__/schemas/OperationData.json'
import { MessageInterpolationTextArea } from '@datahub/components/forms/MessageInterpolationTextArea.tsx'
import type { PanelSpecs } from '@datahub/types.ts'

export const MOCK_OPERATION_SCHEMA: PanelSpecs = {
  schema: schema as RJSFSchema,
  uiSchema: {
    functionId: {
      'ui:widget': 'datahub:function-selector',
    },

    formData: {
      transform: {
        'ui:options': {
          readonly: true,
          removable: false,
          addable: false,
        },
      },
      // TODO[NVL]: Having a single uiSchema is not a good idea; refactor the operation specs
      // Cover MOCK_DATAHUB_FUNCTIONS_MQTT_USER_PROPERTY:
      name: {
        'ui:widget': MessageInterpolationTextArea,
        'ui:options': {
          size: 'sm',
        },
      },
      value: {
        'ui:widget': MessageInterpolationTextArea,
      },
      // Cover MOCK_DATAHUB_FUNCTIONS_SYSTEM_LOG:
      message: {
        'ui:widget': 'datahub:message-interpolation',
      },
      // Cover MOCK_DATAHUB_FUNCTIONS_DELIVERY_REDIRECT
      // - applyPolicy not supporting interpolation (boolean!)
      // Cover MOCK_DATAHUB_FUNCTIONS_SERDES_SERIALIZE
      // Cover MOCK_DATAHUB_FUNCTIONS_SERDES_DESERIALIZE
      // - schemaId not supporting interpolation
      // - schemaVersion not supporting interpolation
      // Cover MOCK_DATAHUB_FUNCTIONS_METRICS_COUNTER_INC
      // - incrementBy not supporting interpolation (integer!)
      // Cover MOCK_DATAHUB_FUNCTIONS_MQTT_DROP
      reasonString: {
        'ui:widget': 'datahub:message-interpolation',
      },
      topic: {
        'ui:widget': registerEntitySelectWidget(CustomFormat.MQTT_TOPIC),
        'ui:options': {
          create: true,
        },
      },
      incrementBy: {
        'ui:widget': 'updown',
      },
      metricName: {
        'ui:widget': 'datahub:metric-counter',
      },
    },
  },
}
