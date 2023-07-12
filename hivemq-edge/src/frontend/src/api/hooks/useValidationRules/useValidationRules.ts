import { RegisterOptions } from 'react-hook-form'
import { useTranslation } from 'react-i18next'

// See https://swagger.io/docs/specification/data-models/data-types/
export const useValidationRules = () => {
  const { t } = useTranslation()

  const getValidationRulesFor = (schema: Record<string, unknown>): RegisterOptions => {
    const options: RegisterOptions = {}

    const assert = (type: 'string' | 'number') => {
      if (schema.type !== type) console.warn(`[openAPI - SyntaxError] Expecting type to be "string"`, { schema })
    }

    if (schema.isRequired) {
      options.required = { value: true, message: t('validation.required') }
    }
    if (schema.maxLength) {
      assert('string')
      const length = schema.maxLength as number
      options.maxLength = { value: length, message: t('validation.maxLength', { count: length }) }
    }
    if (schema.minimum) {
      // TODO[NVL] exclusiveMinimum
      assert('number')
      const minimum = schema.minimum as number
      options.min = { value: minimum, message: t('validation.minimum', { count: minimum }) }
    }
    if (schema.maximum) {
      // TODO[NVL] exclusiveMaximum
      assert('number')
      const maximum = schema.maximum as number
      options.max = { value: maximum, message: t('validation.maximum', { count: maximum }) }
    }
    if (schema.pattern) {
      assert('string')
      try {
        const pattern = new RegExp(schema.pattern as string)
        options.pattern = { value: pattern, message: t('validation.pattern', { pattern: schema.pattern }) }
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
