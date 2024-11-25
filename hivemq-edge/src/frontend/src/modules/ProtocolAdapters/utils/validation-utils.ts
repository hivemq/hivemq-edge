import { FormValidation, RJSFSchema, StrictRJSFSchema, UiSchema } from '@rjsf/utils'
import { Adapter } from '@/api/__generated__'
import { TFunction } from 'i18next'

import { AdapterConfig } from '@/modules/ProtocolAdapters/types.ts'
import { OutwardMapping } from '@/modules/Mappings/types.ts'

import i18n from '@/config/i18n.config.ts'

/**
 *
 * @param jsonSchema
 * @param existingAdapters
 * @param t
 *
 * The custom validation only exposes the form data and the UISchema configuration, NOT the JSONSchema.
 * This is potentially a gap when trying to create custom validation rules.
 *
 * TODO[#93] Principle: custom validation should be based
 *   - first on conditions from the JSONSchema
 *   - second on conditions from the uiSchema
 *   - then (last stand) on conditions from the property name (i.e. formData)
 *
 */
export const customValidate =
  (jsonSchema: RJSFSchema, existingAdapters: Adapter[] | undefined, t: TFunction) =>
  (formData: Record<string, unknown>, errors: FormValidation<AdapterConfig>, uiSchema?: UiSchema<AdapterConfig>) => {
    // Check for uniqueness of `id` ONLY if `format` = `identifier` and not `ui:disabled`
    if (
      !uiSchema?.id?.['ui:disabled'] &&
      (jsonSchema.properties?.['id'] as StrictRJSFSchema)?.format === 'identifier'
    ) {
      if (existingAdapters?.map((e) => e.id).includes(formData.id as string)) {
        errors.id?.addError(t('validation.jsonSchema.identifier.unique'))
      }
    }
    return errors
  }

export const customMappingValidate = (formData: Record<string, OutwardMapping[]>, errors: FormValidation) => {
  const outwardMappings = formData['mqttToOpcuaMappings']

  return outwardMappings.reduce((errors, currentMapping, index) => {
    const { metadata, fieldMapping } = currentMapping
    if (!metadata) {
      errors.mqttToOpcuaMappings?.[index]?.fieldMapping?.addError(
        i18n.t('components:rjsf.MqttTransformationField.validation.error.noValidation')
      )
      return errors
    }

    const { destination } = metadata
    if (!destination) {
      // TODO[NVL] This is not necessarily an error
      errors.mqttToOpcuaMappings?.[index]?.fieldMapping?.addError(
        i18n.t('components:rjsf.MqttTransformationField.validation.error.noSchema')
      )
      return errors
    }

    const countRequired = destination.length

    if (fieldMapping.length !== countRequired) {
      errors.mqttToOpcuaMappings?.[index]?.fieldMapping?.addError(
        i18n.t('components:rjsf.MqttTransformationField.validation.error.missingMapping')
      )
      return errors
    }

    return errors
  }, errors)
}
