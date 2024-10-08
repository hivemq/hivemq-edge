/// <reference types="cypress" />

import { expect } from 'vitest'
import { renderHook } from '@testing-library/react'

import { useMappingValidation } from './useMappingValidation.tsx'

describe('useMappingValidation', () => {
  it('should return default valid state', async () => {
    const { result } = renderHook(useMappingValidation)

    expect(result.current).toStrictEqual({
      status: 'error',
      errors: [],
    })
  })
})
