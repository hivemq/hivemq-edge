import { describe, it, expect, vi } from 'vitest'
import monacoConfig from './monacoConfig'
import type { MonacoInstance } from './types'

// Mock all the language configs
vi.mock('./languages/javascript.config', () => ({
  configureJavaScript: vi.fn(),
}))

vi.mock('./languages/json.config', () => ({
  configureJSON: vi.fn(),
}))

vi.mock('./languages/protobuf.config', () => ({
  configureProtobuf: vi.fn(),
}))

describe('monacoConfig', () => {
  let mockMonaco: MonacoInstance

  beforeEach(() => {
    mockMonaco = {
      editor: {
        defineTheme: vi.fn(),
      },
      languages: {},
    } as unknown as MonacoInstance
  })

  describe('configureLanguages', () => {
    it('should configure all languages', async () => {
      const { configureJavaScript } = await import('./languages/javascript.config')
      const { configureJSON } = await import('./languages/json.config')
      const { configureProtobuf } = await import('./languages/protobuf.config')

      monacoConfig.configureLanguages(mockMonaco)

      expect(configureJavaScript).toHaveBeenCalledWith(mockMonaco)
      expect(configureJSON).toHaveBeenCalledWith(mockMonaco)
      expect(configureProtobuf).toHaveBeenCalledWith(mockMonaco)
    })
  })

  describe('configureThemes', () => {
    it('should configure themes', () => {
      monacoConfig.configureThemes(mockMonaco)

      expect(mockMonaco.editor.defineTheme).toHaveBeenCalled()
    })

    it('should configure themes with options', () => {
      monacoConfig.configureThemes(mockMonaco, { backgroundColor: '#f0f0f0' })

      expect(mockMonaco.editor.defineTheme).toHaveBeenCalled()
    })
  })

  describe('getEditorOptions', () => {
    it('should return editor options for javascript', () => {
      const options = monacoConfig.getEditorOptions('javascript', false)

      expect(options).toHaveProperty('minimap')
      expect(options).toHaveProperty('lineNumbers')
      expect(options).toHaveProperty('scrollBeyondLastLine')
      expect(options).toHaveProperty('quickSuggestions')
    })

    it('should return readonly options when isReadOnly is true', () => {
      const options = monacoConfig.getEditorOptions('javascript', true)

      expect(options.readOnly).toBe(true)
      expect(options).toHaveProperty('minimap')
    })

    it('should return language-specific options for json', () => {
      const options = monacoConfig.getEditorOptions('json', false)

      expect(options).toHaveProperty('quickSuggestions')
      expect(options.wordBasedSuggestions).toBe('off')
    })

    it('should return base options for unknown language', () => {
      const options = monacoConfig.getEditorOptions('unknown', false)

      expect(options).toHaveProperty('minimap')
      expect(options).toHaveProperty('lineNumbers')
    })
  })
})
