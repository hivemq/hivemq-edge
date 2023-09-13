import { describe, expect, vi } from 'vitest'
import { FormValidation, RJSFSchema, UiSchema } from '@rjsf/utils'

import { customValidate } from './validation-utils.ts'
import { Adapter } from '@/api/__generated__'
import { mockAdapter } from '@/api/hooks/useProtocolAdapters/__handlers__'
import { MOCK_ADAPTER_ID } from '@/__test-utils__/mocks.ts'
import { AdapterConfig } from '@/modules/ProtocolAdapters/types.ts'

describe('customValidate()', () => {
  it('should detect duplication of id', () => {
    const mockT = (e: string): string => e
    const mockJSONSchemaId: RJSFSchema = {
      properties: {
        id: {
          format: 'identifier',
        },
      },
    }
    const mockUiSchemaId: UiSchema<AdapterConfig> = {
      id: {
        'ui:disabled': false,
      },
    }
    const mockExistingAdapters: Adapter[] | undefined = [mockAdapter]

    const addError = vi.fn()
    const errors: FormValidation<AdapterConfig> = {
      addError: () => undefined,
      // @ts-ignore
      id: { addError },
    }

    // @ts-ignore
    const customValidateFn = customValidate(mockJSONSchemaId, mockExistingAdapters, mockT)
    expect(customValidateFn).toBeTypeOf('function')
    customValidateFn({ id: MOCK_ADAPTER_ID }, errors, mockUiSchemaId)
    expect(addError).toHaveBeenCalledWith('validation.jsonSchema.identifier.unique')
  })

  it('should NOT check for id', () => {
    const mockT = (e: string): string => e
    const mockJSONSchemaId: RJSFSchema = {
      properties: {},
    }
    const mockUiSchemaId: UiSchema<AdapterConfig> = {
      id: {},
    }
    const mockExistingAdapters: Adapter[] | undefined = [mockAdapter]

    const addError = vi.fn()
    const errors: FormValidation<AdapterConfig> = {
      addError: () => undefined,
      // @ts-ignore
      id: { addError },
    }

    // @ts-ignore
    const customValidateFn = customValidate(mockJSONSchemaId, mockExistingAdapters, mockT)
    expect(customValidateFn).toBeTypeOf('function')
    customValidateFn({ id: MOCK_ADAPTER_ID }, errors, mockUiSchemaId)
    expect(addError).not.toHaveBeenCalledWith('validation.jsonSchema.identifier.unique')
  })

  it('should NOT throw error when id is unique', () => {
    const mockT = (e: string): string => e
    const mockJSONSchemaId: RJSFSchema = {
      properties: {
        id: {
          format: 'identifier',
        },
      },
    }
    const mockUiSchemaId: UiSchema<AdapterConfig> = {
      id: {
        'ui:disabled': false,
      },
    }
    const mockExistingAdapters: Adapter[] | undefined = []

    const addError = vi.fn()
    const errors: FormValidation<AdapterConfig> = {
      addError: () => undefined,
      // @ts-ignore
      id: { addError },
    }

    // @ts-ignore
    const customValidateFn = customValidate(mockJSONSchemaId, mockExistingAdapters, mockT)
    expect(customValidateFn).toBeTypeOf('function')
    customValidateFn({ id: MOCK_ADAPTER_ID }, errors, mockUiSchemaId)
    expect(addError).not.toHaveBeenCalled()
  })
})
