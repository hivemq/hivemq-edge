import { FormValidation, RJSFSchema, StrictRJSFSchema, UiSchema } from '@rjsf/utils'
import { Adapter, SouthboundMapping } from '@/api/__generated__'

import i18n from '@/config/i18n.config.ts'

import { AdapterConfig } from '@/modules/ProtocolAdapters/types.ts'
//
// import {
//   getOutwardMappingRootProperty,
//   getOutwardMappingRootPropertyKey,
// } from '@/modules/Workspace/utils/adapter.utils.ts'

/**
 *
 * @param jsonSchema
 * @param existingAdapters
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
export const customUniqueAdapterValidate =
  (jsonSchema: RJSFSchema, existingAdapters: Adapter[] | undefined) =>
  (formData: Record<string, unknown>, errors: FormValidation<AdapterConfig>, uiSchema?: UiSchema<AdapterConfig>) => {
    // Check for uniqueness of `id` ONLY if `format` = `identifier` and not `ui:disabled`
    if (
      !uiSchema?.id?.['ui:disabled'] &&
      (jsonSchema.properties?.['id'] as StrictRJSFSchema)?.format === 'identifier'
    ) {
      if (existingAdapters?.map((e) => e.id).includes(formData.id as string)) {
        errors.id?.addError(i18n.t('validation.identifier.adapter.unique'))
      }
    }
    return errors
  }

export const customMappingValidate =
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  (_adapterType: string) => (_formData: Record<string, SouthboundMapping[]>, errors: FormValidation) => {
    return errors

    // TODO[⚠ 28441 ⚠] This will not work anymore because of nested structure. DO NOT MERGE AND FIX
    // const key = getOutwardMappingRootProperty(adapterType)
    // const outwardMappingsKey = getOutwardMappingRootPropertyKey(adapterType)
    // // @ts-ignore
    // const outwardMappings = formData[key][outwardMappingsKey] as SouthboundMapping[]
    //
    // if (!outwardMappings.length) return errors
    //
    // // return outwardMappings.reduce((errors, currentMapping, index) => {
    // return outwardMappings.reduce((errors) => {
    //   // const { metadata, fieldMapping } = currentMapping
    //   // if (!metadata) {
    //   //   errors?.[key]?.[outwardMappingsKey]?.[index]?.fieldMapping?.addError(
    //   //     i18n.t('components:rjsf.MqttTransformationField.validation.error.noValidation')
    //   //   )
    //   //   return errors
    //   // }
    //   //
    //   // const { destination } = metadata
    //   // if (!destination) {
    //   //   // TODO[NVL] This is not necessarily an error
    //   //   errors?.[key]?.[outwardMappingsKey]?.[index]?.fieldMapping?.addError(
    //   //     i18n.t('components:rjsf.MqttTransformationField.validation.error.noSchema')
    //   //   )
    //   //   return errors
    //   // }
    //   //
    //   // const countRequired = destination.length
    //   //
    //   // if (fieldMapping?.length !== countRequired) {
    //   //   errors?.[key]?.[outwardMappingsKey]?.[index]?.fieldMapping?.addError(
    //   //     i18n.t('components:rjsf.MqttTransformationField.validation.error.missingMapping')
    //   //   )
    //   //   return errors
    //   // }
    //
    //   return errors
    // }, errors)
  }
