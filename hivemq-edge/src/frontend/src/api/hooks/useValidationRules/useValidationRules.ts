import { RegisterOptions } from 'react-hook-form'
import { useTranslation } from 'react-i18next'

export const useValidationRules = () => {
  const { t } = useTranslation()

  const getValidationRulesFor = (schema: Record<string, unknown>): RegisterOptions => {
    const options: RegisterOptions = {}

    if (schema.isRequired) {
      options.required = { value: true, message: t('validation.required') }
    }
    if (schema.maxLength) {
      const length = schema.maxLength as number
      options.maxLength = { value: length, message: t('validation.maxLength', { count: length }) }
    }
    if (schema.minimum) {
      // TODO[NVL] exclusiveMinimum
      const minimum = schema.minimum as number
      options.min = { value: minimum, message: t('validation.minimum', { count: minimum }) }
    }
    if (schema.maximum) {
      // TODO[NVL] exclusiveMaximum
      const maximum = schema.maximum as number
      options.max = { value: maximum, message: t('validation.maximum', { count: maximum }) }
    }
    if (schema.pattern) {
      try {
        const maximum = new RegExp(schema.pattern as string)
        options.pattern = { value: maximum, message: t('validation.pattern', { pattern: schema.pattern }) }
      } catch (e: unknown) {
        const error = e as Error
        console.warn(`[openAPI - ${error.name}]`, error.message)
      }
    }
    // TODO[NVL] multipleOf, minLength
    return options
  }

  return getValidationRulesFor
}
