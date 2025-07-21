import type { FormValidation, UiSchema } from '@rjsf/utils'
import { getUiOptions } from '@rjsf/utils'
import i18n from '@/config/i18n.config.ts'

export const customUniqueBridgeValidate =
  (existingBridges: string[] | undefined) =>
  (formData: Record<string, unknown>, errors: FormValidation, uiSchema?: UiSchema) => {
    const { disabled, isNewBridge } = getUiOptions(uiSchema?.['id'])

    if (disabled === false && isNewBridge) {
      if (existingBridges && existingBridges.includes(formData.id as string)) {
        errors.id?.addError(i18n.t('validation.identifier.bridge.unique'))
      }
    }
    return errors
  }
