import { describe, it, expect, vi, beforeEach } from 'vitest'
import { configureJavaScript } from './javascript.config'
import type { MonacoInstance } from '../types'

// Mock the type definitions import
vi.mock('./datahub-transforms.d.ts?raw', () => ({
  default: 'interface Publish { topic: string; }',
}))

describe('JavaScript Config', () => {
  let mockMonaco: MonacoInstance

  beforeEach(() => {
    mockMonaco = {
      languages: {
        typescript: {
          ScriptTarget: {
            ES2020: 7,
          },
          ModuleResolutionKind: {
            NodeJs: 2,
          },
          ModuleKind: {
            CommonJS: 1,
          },
          javascriptDefaults: {
            setEagerModelSync: vi.fn(),
            setDiagnosticsOptions: vi.fn(),
            setCompilerOptions: vi.fn(),
            addExtraLib: vi.fn(),
          },
        },
        registerCodeActionProvider: vi.fn(),
      },
    } as unknown as MonacoInstance
  })

  it('should configure JavaScript defaults', () => {
    configureJavaScript(mockMonaco)

    expect(mockMonaco.languages.typescript.javascriptDefaults.setDiagnosticsOptions).toHaveBeenCalled()
    expect(mockMonaco.languages.typescript.javascriptDefaults.setCompilerOptions).toHaveBeenCalled()
  })

  it('should set diagnostics options', () => {
    configureJavaScript(mockMonaco)

    const call = (mockMonaco.languages.typescript.javascriptDefaults.setDiagnosticsOptions as ReturnType<typeof vi.fn>)
      .mock.calls[0][0]

    expect(call.noSemanticValidation).toBe(false)
    expect(call.noSyntaxValidation).toBe(false)
    expect(call.diagnosticCodesToIgnore).toBeInstanceOf(Array)
  })

  it('should set compiler options for JavaScript', () => {
    configureJavaScript(mockMonaco)

    const call = (mockMonaco.languages.typescript.javascriptDefaults.setCompilerOptions as ReturnType<typeof vi.fn>)
      .mock.calls[0][0]

    expect(call.allowNonTsExtensions).toBe(true)
    expect(call.allowJs).toBe(true)
    expect(call.noLib).toBe(false)
  })

  it('should add extra lib for DataHub types', () => {
    configureJavaScript(mockMonaco)

    expect(mockMonaco.languages.typescript.javascriptDefaults.addExtraLib).toHaveBeenCalled()
  })
})
