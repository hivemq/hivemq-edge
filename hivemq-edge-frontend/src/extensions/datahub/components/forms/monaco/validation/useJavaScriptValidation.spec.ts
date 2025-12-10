import { describe, it, expect, vi } from 'vitest'
import { renderHook } from '@testing-library/react'
import { useJavaScriptValidation } from './useJavaScriptValidation'
import * as monacoReact from '@monaco-editor/react'
import type { Monaco } from '@monaco-editor/react'

// Mock @monaco-editor/react
vi.mock('@monaco-editor/react', () => ({
  useMonaco: vi.fn(),
}))

// Mock the validator
vi.mock('./javascriptValidator', () => ({
  validateJavaScript: vi.fn(),
}))

describe('useJavaScriptValidation', () => {
  let mockMonaco: Monaco

  beforeEach(() => {
    // Minimal Monaco mock - only what's needed for the hook
    mockMonaco = {
      editor: {
        createModel: vi.fn(),
        getModel: vi.fn(),
        getModelMarkers: vi.fn(),
      },
      Uri: {
        parse: vi.fn(),
      },
      MarkerSeverity: {
        Error: 8,
        Warning: 4,
        Info: 2,
        Hint: 1,
      },
    } as unknown as Monaco
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  it('should return validate function and isReady state', () => {
    vi.mocked(monacoReact.useMonaco).mockReturnValue(mockMonaco)

    const { result } = renderHook(() => useJavaScriptValidation())

    expect(result.current).toHaveProperty('validate')
    expect(result.current).toHaveProperty('isReady')
    expect(typeof result.current.validate).toBe('function')
    expect(result.current.isReady).toBe(true)
  })

  it('should indicate not ready when Monaco is not loaded', () => {
    vi.mocked(monacoReact.useMonaco).mockReturnValue(null)

    const { result } = renderHook(() => useJavaScriptValidation())

    expect(result.current.isReady).toBe(false)
  })

  it('should call validateJavaScript when validate is called with Monaco loaded', async () => {
    const { validateJavaScript } = await import('./javascriptValidator')
    vi.mocked(validateJavaScript).mockResolvedValue({
      isValid: true,
      errors: [],
      warnings: [],
    })
    vi.mocked(monacoReact.useMonaco).mockReturnValue(mockMonaco)

    const { result } = renderHook(() => useJavaScriptValidation())

    const code = 'function test() { return true; }'
    await result.current.validate(code)

    expect(validateJavaScript).toHaveBeenCalledWith(mockMonaco, code)
  })

  it('should return valid result when Monaco is not loaded', async () => {
    vi.mocked(monacoReact.useMonaco).mockReturnValue(null)

    const { result } = renderHook(() => useJavaScriptValidation())

    const code = 'function test() { return true; }'
    const validationResult = await result.current.validate(code)

    expect(validationResult.isValid).toBe(true)
    expect(validationResult.errors).toHaveLength(0)
    expect(validationResult.warnings).toHaveLength(0)
  })

  it('should return validation errors when code is invalid', async () => {
    const { validateJavaScript } = await import('./javascriptValidator')
    const mockErrors = [
      {
        message: "'}' expected.",
        line: 1,
        column: 18,
        severity: 'error' as const,
      },
    ]
    vi.mocked(validateJavaScript).mockResolvedValue({
      isValid: false,
      errors: mockErrors,
      warnings: [],
    })
    vi.mocked(monacoReact.useMonaco).mockReturnValue(mockMonaco)

    const { result } = renderHook(() => useJavaScriptValidation())

    const code = 'function test() {'
    const validationResult = await result.current.validate(code)

    expect(validationResult.isValid).toBe(false)
    expect(validationResult.errors).toEqual(mockErrors)
  })

  it('should maintain stable validate function reference', () => {
    vi.mocked(monacoReact.useMonaco).mockReturnValue(mockMonaco)

    const { result, rerender } = renderHook(() => useJavaScriptValidation())

    const firstValidate = result.current.validate

    rerender()

    const secondValidate = result.current.validate

    expect(firstValidate).toBe(secondValidate)
  })

  it('should update validate function when Monaco instance changes', () => {
    vi.mocked(monacoReact.useMonaco).mockReturnValue(null)

    const { result, rerender } = renderHook(() => useJavaScriptValidation())

    const firstValidate = result.current.validate
    expect(result.current.isReady).toBe(false)

    // Monaco loads
    vi.mocked(monacoReact.useMonaco).mockReturnValue(mockMonaco)
    rerender()

    const secondValidate = result.current.validate
    expect(result.current.isReady).toBe(true)
    expect(firstValidate).not.toBe(secondValidate)
  })
})
