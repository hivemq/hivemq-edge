import { renderHook } from '@testing-library/react'
import { describe, expect } from 'vitest'
import useGetUiSchema from './useGetUISchema.ts'
import '@/config/i18n.config.ts'

describe('useGetUiSchema()', () => {
  beforeEach(() => {
    window.localStorage.clear()
  })

  it('should render the id property editable', () => {
    const { result } = renderHook(() => useGetUiSchema())

    expect(result.current).toEqual(expect.objectContaining({ id: expect.objectContaining({ 'ui:disabled': false }) }))
  })

  it('should render the id property disabled', () => {
    const { result } = renderHook(() => useGetUiSchema(false))

    expect(result.current).toEqual(expect.objectContaining({ id: expect.objectContaining({ 'ui:disabled': true }) }))
  })
})
