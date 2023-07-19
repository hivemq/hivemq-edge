import { vi, expect } from 'vitest'
import { renderHook } from '@testing-library/react'

import { useValidationRules } from '@/api/hooks/useValidationRules/useValidationRules.ts'
import '@/config/i18n.config.ts'
import { RegisterOptions } from 'react-hook-form'

interface Suite {
  schema: Record<string, unknown>
  expected: RegisterOptions
  shouldWarn?: boolean
}

const validationSuite: Suite[] = [
  {
    schema: {},
    expected: {},
  },
  {
    schema: { test: 'random value' },
    expected: {},
  },
  {
    schema: {
      type: 'string',
      isRequired: true,
    },
    expected: {
      required: { value: true, message: 'This property is required' },
    },
  },
  {
    schema: { type: 'fakeString', maxLength: 255 },
    expected: {
      maxLength: { value: 255, message: 'No more than 255 characters allowed for the property' },
    },
    shouldWarn: true,
  },
  {
    schema: { type: 'string', maxLength: 255 },
    expected: {
      maxLength: { value: 255, message: 'No more than 255 characters allowed for the property' },
    },
  },
  {
    schema: { type: 'number', minimum: 10 },
    expected: {
      min: { value: 10, message: 'Should be at least 10' },
    },
  },
  {
    schema: { type: 'number', maximum: 10 },
    expected: {
      max: { value: 10, message: 'Should not be more than 10' },
    },
  },
  {
    schema: { type: 'string', pattern: '[a]' },
    expected: {
      // TODO Warning, not a deep check on RegExp
      pattern: { value: new RegExp('[a]'), message: 'Should match the regular expression [a]' },
    },
  },

  {
    schema: { type: 'string', pattern: '(a' },
    expected: {},
    shouldWarn: true,
  },
]

describe('useValidationRules', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it.each<Suite>(validationSuite)('$schema', ({ schema, expected, shouldWarn }) => {
    const { result } = renderHook(useValidationRules)
    const getValidationRulesFor = result.current

    vi.spyOn(console, 'warn')

    expect(1)
    expect(getValidationRulesFor(schema)).toMatchObject(expected)
    expect(console.warn).toHaveBeenCalledTimes(shouldWarn ? 1 : 0)
  })
})
