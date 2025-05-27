import { describe, expect } from 'vitest'

import { getRequiredUiSchema } from '@/modules/ProtocolAdapters/utils/uiSchema.utils.ts'
import type { UiSchema } from '@rjsf/utils'

describe('getRequiredUiSchema', () => {
  it('should return the mandatory properties ', () => {
    expect(getRequiredUiSchema(undefined, true)).toStrictEqual(
      expect.objectContaining({
        id: {
          'ui:disabled': false,
        },
        'ui:submitButtonOptions': {
          norender: true,
        },
      })
    )
    expect(getRequiredUiSchema(undefined, false)).toStrictEqual(
      expect.objectContaining({
        id: {
          'ui:disabled': true,
        },
        'ui:submitButtonOptions': {
          norender: true,
        },
      })
    )
  })

  it('should merge adapter with mandatory properties', () => {
    const uiSchema: UiSchema = {
      id: {
        'ui:disabled': true,
        title: 'new title',
      },
      'ui:submitButtonOptions': {
        norender: false,
        submitText: 'Submit',
      },
      test: {
        'ui:disabled': true,
      },
    }

    expect(getRequiredUiSchema(uiSchema, true)).toStrictEqual({
      test: {
        'ui:disabled': true,
      },
      id: {
        title: 'new title',
        'ui:disabled': false,
      },
      'ui:submitButtonOptions': {
        norender: true,
        submitText: 'Submit',
      },
    })
  })
})
