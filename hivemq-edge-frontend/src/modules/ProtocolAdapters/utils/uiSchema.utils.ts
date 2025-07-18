import { CustomFormat } from '@/api/types/json-schema.ts'
import { registerEntitySelectWidget } from '@/components/rjsf/Widgets/EntitySelectWidget.tsx'
import type { RegistryFieldsType, RegistryWidgetsType, UiSchema } from '@rjsf/utils'

import { CompactArrayField, MqttTransformationField } from '@/components/rjsf/Fields'

import { JSONSchemaEditor } from '@datahub/components/forms'

export const getRequiredUiSchema = (uiSchema: UiSchema | undefined, isNewAdapter: boolean): UiSchema => {
  const { ['ui:submitButtonOptions']: submitButtonOptions, id, ...rest } = uiSchema || {}
  const newSchema: UiSchema = {
    'ui:submitButtonOptions': {
      // required to relocate the submit button outside the form
      ...submitButtonOptions,
      norender: true,
    },
    id: {
      // required to prevent custom validation when editing an existing adapter
      ...id,
      'ui:disabled': !isNewAdapter,
    },
    ...rest,
  }

  return newSchema
}

export const adapterJSFWidgets: RegistryWidgetsType = {
  // @ts-ignore [24369] Turn discovery browser off (and replace by regular text input)
  'discovery:tagBrowser': 'text',
  'application/schema+json': JSONSchemaEditor,
  'mqtt-tag': registerEntitySelectWidget(CustomFormat.MQTT_TAG),
  'mqtt-topic-filter': registerEntitySelectWidget(CustomFormat.MQTT_TOPIC_FILTER),
  'mqtt-topic': registerEntitySelectWidget(CustomFormat.MQTT_TOPIC),
}

export const adapterJSFFields: RegistryFieldsType = {
  compactTable: CompactArrayField,
  'mqtt:transform': MqttTransformationField,
}
