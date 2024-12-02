import { RegistryFieldsType, RegistryWidgetsType, UiSchema } from '@rjsf/utils'
import { AlertStatus } from '@chakra-ui/react'

import { CompactArrayField, InternalNotice, MqttTransformationField } from '@/components/rjsf/Fields'

import i18n from '@/config/i18n.config.ts'
import { JSONSchemaEditor } from '@datahub/components/forms'
import { registerEntitySelectWidget } from '@/components/rjsf/Widgets/EntitySelectWidget.tsx'
import { CustomFormat } from '@/api/types/json-schema.ts'

export const getRequiredUiSchema = (
  uiSchema: UiSchema | undefined,
  isNewAdapter: boolean,
  hideProperties?: string[]
): UiSchema => {
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

  if (hideProperties) {
    for (const property of hideProperties) {
      const status: AlertStatus = 'info'
      newSchema[property] = {
        'ui:field': 'text:warning',
        'ui:options': {
          status,
          message: i18n.t('warnings.featureFlag.splitSchema'),
        },
      }
    }
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
  'text:warning': InternalNotice,
  'mqtt:transform': MqttTransformationField,
}
