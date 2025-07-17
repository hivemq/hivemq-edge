import type { Bridge } from '@/api/__generated__'
import { mockBridge, mockBridgeId } from '@/api/hooks/useGetBridges/__handlers__'
import { customUniqueBridgeValidate } from '@/modules/Bridges/utils/validation-utils.ts'
import type { FormValidation, UiSchema } from '@rjsf/utils'
import { describe, it, expect, vi } from 'vitest'

describe('customUniqueBridgeValidate', () => {
  it('should detect duplication of id', () => {
    const mockExistingBridges: Bridge[] | undefined = [mockBridge]
    const moclkUiSchema: UiSchema = {
      id: {
        'ui:disabled': false,
        'ui:options': { isNewBridge: true },
      },
    }

    const addError = vi.fn()
    const errors: FormValidation = {
      addError: () => undefined,
      // @ts-ignore
      id: { addError },
    }

    expect(addError).not.toHaveBeenCalled()
    const customValidateFn = customUniqueBridgeValidate(mockExistingBridges.map((e) => e.id))
    expect(customValidateFn).toBeTypeOf('function')
    customValidateFn({ id: mockBridgeId }, errors, moclkUiSchema)
    expect(addError).toHaveBeenCalledWith('This identifier is already in use for another bridge')
  })

  it('should not detect duplication of id in edit mode', () => {
    const mockExistingBridges: Bridge[] | undefined = [mockBridge]
    const mockUiSchema: UiSchema = {
      id: {
        'ui:disabled': true,
        'ui:options': { isNewBridge: false },
      },
    }

    const addError = vi.fn()
    const errors: FormValidation = {
      addError: () => undefined,
      // @ts-ignore
      id: { addError },
    }

    expect(addError).not.toHaveBeenCalled()

    const customValidateFn = customUniqueBridgeValidate(mockExistingBridges.map((e) => e.id))
    expect(customValidateFn).toBeTypeOf('function')
    customValidateFn({ id: mockBridgeId }, errors, mockUiSchema)
    expect(addError).not.toHaveBeenCalled()
  })
})
