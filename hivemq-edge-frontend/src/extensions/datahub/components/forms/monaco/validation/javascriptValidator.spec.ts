import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import type { Monaco } from '@monaco-editor/react'
import type * as monacoType from 'monaco-editor'
import { validateJavaScript, formatValidationError, hasSyntaxErrors } from './javascriptValidator'

describe('javascriptValidator', () => {
  let mockMonaco: Monaco
  let mockModel: monacoType.editor.ITextModel
  let modelDisposeSpy: ReturnType<typeof vi.fn>

  beforeEach(() => {
    modelDisposeSpy = vi.fn()

    // Create minimal mock model that satisfies ITextModel interface
    mockModel = {
      uri: { toString: () => 'inmemory://model/validation.js' } as monacoType.Uri,
      dispose: modelDisposeSpy,
      getValue: vi.fn(() => ''),
      getLineCount: vi.fn(() => 1),
      getLineContent: vi.fn(() => ''),
      getLineLength: vi.fn(() => 0),
      getLineMinColumn: vi.fn(() => 1),
      getLineMaxColumn: vi.fn(() => 1),
      getLineFirstNonWhitespaceColumn: vi.fn(() => 1),
      getLineLastNonWhitespaceColumn: vi.fn(() => 1),
    } as unknown as monacoType.editor.ITextModel

    // Create Monaco mock with proper types
    mockMonaco = {
      Uri: {
        parse: vi.fn((uri: string) => ({ toString: () => uri }) as monacoType.Uri),
      },
      editor: {
        createModel: vi.fn((_code: string, _language: string, uri: monacoType.Uri) => {
          return {
            ...mockModel,
            uri,
          }
        }),
        getModel: vi.fn(() => null),
        getModelMarkers: vi.fn(() => []),
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

  describe('validateJavaScript', () => {
    it('should validate correct JavaScript code', async () => {
      const code = 'function test() { return true; }'

      const result = await validateJavaScript(mockMonaco, code)

      expect(result.isValid).toBe(true)
      expect(result.errors).toHaveLength(0)
      expect(result.warnings).toHaveLength(0)
      expect(mockMonaco.editor.createModel).toHaveBeenCalledWith(code, 'javascript', expect.anything())
    })

    it('should detect syntax errors', async () => {
      const code = 'function test() {'
      const mockMarkers = [
        {
          message: "'}' expected.",
          startLineNumber: 1,
          startColumn: 18,
          severity: mockMonaco.MarkerSeverity.Error,
          code: '1005',
        },
      ]

      vi.mocked(mockMonaco.editor.getModelMarkers).mockReturnValue(mockMarkers as never)

      const result = await validateJavaScript(mockMonaco, code)

      expect(result.isValid).toBe(false)
      expect(result.errors).toHaveLength(1)
      expect(result.errors[0]).toEqual({
        message: "'}' expected.",
        line: 1,
        column: 18,
        severity: 'error',
        code: '1005',
      })
    })

    it('should handle warnings separately from errors', async () => {
      const code = 'var x = 1; var x = 2;'
      const mockMarkers = [
        {
          message: "Cannot redeclare block-scoped variable 'x'.",
          startLineNumber: 1,
          startColumn: 16,
          severity: mockMonaco.MarkerSeverity.Error,
          code: '2451',
        },
        {
          message: "'x' is declared but its value is never read.",
          startLineNumber: 1,
          startColumn: 5,
          severity: mockMonaco.MarkerSeverity.Warning,
          code: '6133',
        },
      ]

      vi.mocked(mockMonaco.editor.getModelMarkers).mockReturnValue(mockMarkers as never)

      const result = await validateJavaScript(mockMonaco, code)

      expect(result.isValid).toBe(false) // Has errors
      expect(result.errors).toHaveLength(1)
      expect(result.warnings).toHaveLength(1)
      expect(result.errors[0].severity).toBe('error')
      expect(result.warnings[0].severity).toBe('warning')
    })

    it('should dispose temporary model after validation', async () => {
      const code = 'function test() { return true; }'

      await validateJavaScript(mockMonaco, code)

      expect(modelDisposeSpy).toHaveBeenCalled()
    })

    it('should dispose existing model before creating new one', async () => {
      const existingModel = {
        dispose: vi.fn(),
      } as unknown as monacoType.editor.ITextModel

      vi.mocked(mockMonaco.editor.getModel).mockReturnValue(existingModel)

      const code = 'function test() { return true; }'
      await validateJavaScript(mockMonaco, code)

      expect(existingModel.dispose).toHaveBeenCalled()
    })

    it('should use custom URI if provided', async () => {
      const code = 'function test() {}'
      const customUri = 'inmemory://custom/path.js'

      await validateJavaScript(mockMonaco, code, customUri)

      expect(mockMonaco.Uri.parse).toHaveBeenCalledWith(customUri)
    })

    it('should handle empty code', async () => {
      const code = ''

      const result = await validateJavaScript(mockMonaco, code)

      expect(result.isValid).toBe(true)
      expect(result.errors).toHaveLength(0)
    })

    it('should validate DataHub transform function signature', async () => {
      const code = 'function transform(publish, context) { return publish; }'

      const result = await validateJavaScript(mockMonaco, code)

      expect(result.isValid).toBe(true)
      expect(mockMonaco.editor.createModel).toHaveBeenCalledWith(code, 'javascript', expect.anything())
    })

    it('should handle multiple syntax errors', async () => {
      const code = 'function test() { const x = '
      const mockMarkers = [
        {
          message: 'Expression expected.',
          startLineNumber: 1,
          startColumn: 29,
          severity: mockMonaco.MarkerSeverity.Error,
          code: '1109',
        },
        {
          message: "'}' expected.",
          startLineNumber: 1,
          startColumn: 29,
          severity: mockMonaco.MarkerSeverity.Error,
          code: '1005',
        },
      ]

      vi.mocked(mockMonaco.editor.getModelMarkers).mockReturnValue(mockMarkers as never)

      const result = await validateJavaScript(mockMonaco, code)

      expect(result.isValid).toBe(false)
      expect(result.errors).toHaveLength(2)
    })
  })

  describe('formatValidationError', () => {
    it('should format error with line and column', () => {
      const error = {
        message: "'}' expected.",
        line: 1,
        column: 18,
        severity: 'error' as const,
        code: '1005',
      }

      const formatted = formatValidationError(error)

      expect(formatted).toBe("Line 1, Column 18: '}' expected.")
    })

    it('should handle multi-line errors', () => {
      const error = {
        message: 'Unexpected token',
        line: 42,
        column: 15,
        severity: 'error' as const,
      }

      const formatted = formatValidationError(error)

      expect(formatted).toBe('Line 42, Column 15: Unexpected token')
    })
  })

  describe('hasSyntaxErrors', () => {
    it('should return true when errors exist', () => {
      const result = {
        isValid: false,
        errors: [
          {
            message: 'Error',
            line: 1,
            column: 1,
            severity: 'error' as const,
          },
        ],
        warnings: [],
      }

      expect(hasSyntaxErrors(result)).toBe(true)
    })

    it('should return false when no errors exist', () => {
      const result = {
        isValid: true,
        errors: [],
        warnings: [],
      }

      expect(hasSyntaxErrors(result)).toBe(false)
    })

    it('should return false when only warnings exist', () => {
      const result = {
        isValid: true,
        errors: [],
        warnings: [
          {
            message: 'Warning',
            line: 1,
            column: 1,
            severity: 'warning' as const,
          },
        ],
      }

      expect(hasSyntaxErrors(result)).toBe(false)
    })
  })
})
