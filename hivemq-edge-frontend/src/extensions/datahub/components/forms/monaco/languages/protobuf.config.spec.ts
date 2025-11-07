import type { MockInstance } from 'vitest'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { configureProtobuf } from './protobuf.config'
import type { MonacoInstance } from '../types'

describe('Protobuf Config', () => {
  let mockMonaco: MonacoInstance

  beforeEach(() => {
    mockMonaco = {
      languages: {
        getLanguages: vi.fn(),
        register: vi.fn(),
        setMonarchTokensProvider: vi.fn(),
        setLanguageConfiguration: vi.fn(),
        registerCompletionItemProvider: vi.fn(),
        CompletionItemKind: {
          Keyword: 1,
          TypeParameter: 2,
        },
      },
    } as unknown as MonacoInstance

    vi.spyOn(console, 'log').mockImplementation(() => {})
  })

  it('should register custom protobuf language when not built-in', () => {
    ;(mockMonaco.languages.getLanguages as unknown as MockInstance).mockReturnValue([])

    configureProtobuf(mockMonaco)

    expect(mockMonaco.languages.register).toHaveBeenCalledWith({ id: 'proto' })
    expect(mockMonaco.languages.setMonarchTokensProvider).toHaveBeenCalledWith('proto', expect.any(Object))
    expect(mockMonaco.languages.setLanguageConfiguration).toHaveBeenCalledWith('proto', expect.any(Object))
  })

  it('should add completion provider when built-in support exists', () => {
    ;(mockMonaco.languages.getLanguages as unknown as MockInstance).mockReturnValue([{ id: 'proto' }])

    configureProtobuf(mockMonaco)

    expect(mockMonaco.languages.register).not.toHaveBeenCalled()
    expect(mockMonaco.languages.registerCompletionItemProvider).toHaveBeenCalledWith('proto', expect.any(Object))
  })

  it('should log when built-in protobuf support is detected', () => {
    ;(mockMonaco.languages.getLanguages as unknown as MockInstance).mockReturnValue([{ id: 'protobuf' }])

    configureProtobuf(mockMonaco)

    expect(console.log).toHaveBeenCalledWith(expect.stringContaining('Built-in protobuf support detected'))
  })

  it('should log when registering custom protobuf language', () => {
    ;(mockMonaco.languages.getLanguages as unknown as MockInstance).mockReturnValue([])

    configureProtobuf(mockMonaco)

    expect(console.log).toHaveBeenCalledWith(expect.stringContaining('Registering protobuf language'))
  })

  it('should register completion provider with keywords and types', () => {
    ;(mockMonaco.languages.getLanguages as unknown as MockInstance).mockReturnValue([])

    configureProtobuf(mockMonaco)

    expect(console.log).toHaveBeenCalledWith(
      expect.stringContaining('Completion provider registered with'),
      expect.any(Number),
      expect.stringContaining('keywords'),
      expect.any(Number),
      expect.stringContaining('types')
    )
  })
})
