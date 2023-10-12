import { FormValidation, UiSchema, RJSFSchema, StrictRJSFSchema } from '@rjsf/utils'
import { Adapter } from '@/api/__generated__'
import { TFunction } from 'i18next'

import { AdapterConfig } from '@/modules/ProtocolAdapters/types.ts'
import { customizeValidator } from '@rjsf/validator-ajv8'

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

export const customFormatsValidator = customizeValidator({
  customFormats: {
    'mqtt-topic': /^[^+#$]*$/,
  },
})
