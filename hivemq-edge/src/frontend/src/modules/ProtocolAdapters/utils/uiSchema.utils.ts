import { RegistryFieldsType, RegistryWidgetsType, UiSchema } from '@rjsf/utils'
import { AlertStatus } from '@chakra-ui/react'

import { CompactArrayField, InternalNotice, MqttTransformationField } from '@/components/rjsf/Fields'

import i18n from '@/config/i18n.config.ts'

export const getRequiredUiSchema = (
  uiSchema: UiSchema | undefined,
  isNewAdapter: boolean,
  hideSubscriptions?: string
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

  if (hideSubscriptions) {
    const status: AlertStatus = 'info'
    newSchema[hideSubscriptions] = {
      'ui:field': 'text:warning',
      'ui:options': {
        status,
        message: i18n.t('warnings.featureFlag.splitSchema'),
      },
    }
  }

  return newSchema
}

export const adapterJSFWidgets: RegistryWidgetsType = {
  // @ts-ignore [24369] Turn discovery browser off (and replace by regular text input)
  'discovery:tagBrowser': 'text',
}

export const adapterJSFFields: RegistryFieldsType = {
  compactTable: CompactArrayField,
  'text:warning': InternalNotice,
  'mqtt:transform': MqttTransformationField,
}
