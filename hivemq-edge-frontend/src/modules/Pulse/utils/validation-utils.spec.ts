import { MOCK_PULSE_ASSET, MOCK_PULSE_ASSET_MAPPED_UNIQUE } from '@/api/hooks/usePulse/__handlers__'
import { customSchemaValidator } from '@/modules/Pulse/utils/validation-utils.ts'
import { describe, expect, vi } from 'vitest'
import { createErrorHandler } from '@rjsf/utils'

describe('customSchemaValidator', () => {
  it('should detect errors in  schema', () => {
    const addError = vi.fn()
    const errors = createErrorHandler(MOCK_PULSE_ASSET_MAPPED_UNIQUE)
    if (errors.schema?.addError) errors.schema.addError = addError

    customSchemaValidator(MOCK_PULSE_ASSET_MAPPED_UNIQUE, errors)
    expect(errors.schema?.addError).toHaveBeenCalledWith(
      "Not a valid JSONSchema: `properties` doesn't contain any properties"
    )
  })

  it('should not trigger errors for valid schema', () => {
    const addError = vi.fn()
    const errors = createErrorHandler(MOCK_PULSE_ASSET_MAPPED_UNIQUE)
    if (errors.schema?.addError) errors.schema.addError = addError
    customSchemaValidator(MOCK_PULSE_ASSET, errors)
    expect(errors.schema?.addError).not.toHaveBeenCalled()
  })
})
