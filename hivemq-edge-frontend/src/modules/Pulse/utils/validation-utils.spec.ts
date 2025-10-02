import { MOCK_COMBINER_ASSET } from '@/api/hooks/useCombiners/__handlers__'
import {
  MOCK_PULSE_ASSET,
  MOCK_PULSE_ASSET_MAPPED_DUPLICATE,
  MOCK_PULSE_ASSET_MAPPED_UNIQUE,
} from '@/api/hooks/usePulse/__handlers__'
import { customSchemaValidator } from '@/modules/Pulse/utils/validation-utils.ts'
import { describe, expect, vi } from 'vitest'
import { createErrorHandler } from '@rjsf/utils'

const createErrors = () => {
  const errors = createErrorHandler(MOCK_PULSE_ASSET_MAPPED_UNIQUE)
  if (errors.schema?.addError) errors.schema.addError = vi.fn()
  if (errors.mapping?.mappingId?.addError) errors.mapping.mappingId.addError = vi.fn()
  return errors
}

describe('customSchemaValidator', () => {
  it('should detect errors in  schema', () => {
    const errors = createErrors()

    customSchemaValidator(MOCK_COMBINER_ASSET)(MOCK_PULSE_ASSET_MAPPED_UNIQUE, errors)
    expect(errors.schema?.addError).toHaveBeenCalledWith(
      "Not a valid JSONSchema: `properties` doesn't contain any properties"
    )
    expect(errors.mapping?.mappingId?.addError).not.toHaveBeenCalled()
  })

  it('should not trigger errors for valid schema', () => {
    const errors = createErrors()

    customSchemaValidator(MOCK_COMBINER_ASSET)(MOCK_PULSE_ASSET, errors)
    expect(errors.schema?.addError).not.toHaveBeenCalled()
    expect(errors.mapping?.mappingId?.addError).not.toHaveBeenCalled()
  })

  it('should detect errors for missing combiner or mapping', () => {
    const errors = createErrors()

    customSchemaValidator(undefined)(MOCK_PULSE_ASSET_MAPPED_DUPLICATE, errors)
    expect(errors.schema?.addError).not.toHaveBeenCalled()
    expect(errors.mapping?.mappingId?.addError).toHaveBeenCalledWith('The specified mapping cannot be found')
  })
})
