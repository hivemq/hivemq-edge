import { describe, it, expect, vi, beforeEach } from 'vitest'
import {
  addDataHubActionsToEditor,
  registerDataHubCommands,
  registerDataHubCodeActions,
  configureDataHubFeatures,
} from './datahub-commands'
import type { MonacoInstance } from '../types'
import type { editor } from 'monaco-editor'

// Mock the template import
vi.mock('../__test-utils__/transform-template.js?raw', () => ({
  default: '// Template code\nfunction transform() {}',
}))

describe('DataHub Commands', () => {
  let mockEditor: {
    addAction: ReturnType<typeof vi.fn>
    getValue: ReturnType<typeof vi.fn>
    setValue: ReturnType<typeof vi.fn>
    getSelection: ReturnType<typeof vi.fn>
    executeEdits: ReturnType<typeof vi.fn>
    getModel: ReturnType<typeof vi.fn>
    getAction: ReturnType<typeof vi.fn>
  }
  let mockMonaco: MonacoInstance

  beforeEach(() => {
    mockEditor = {
      addAction: vi.fn(),
      getValue: vi.fn().mockReturnValue(''),
      setValue: vi.fn(),
      getSelection: vi.fn().mockReturnValue(null),
      executeEdits: vi.fn(),
      getModel: vi.fn(),
      getAction: vi.fn(),
    }

    mockMonaco = {
      KeyMod: {
        CtrlCmd: 2048,
        Shift: 1024,
      },
      KeyCode: {
        KeyI: 39,
      },
      languages: {
        registerCodeActionProvider: vi.fn(),
      },
    } as unknown as MonacoInstance
  })

  describe('registerDataHubCommands', () => {
    it('should complete command registration', () => {
      expect(() => registerDataHubCommands()).not.toThrow()
    })
  })

  describe('addDataHubActionsToEditor', () => {
    it('should add template insertion action to editor', () => {
      addDataHubActionsToEditor(mockEditor as unknown as editor.IStandaloneCodeEditor, mockMonaco)

      expect(mockEditor.addAction).toHaveBeenCalledWith(
        expect.objectContaining({
          id: 'datahub.insertTransformTemplate',
          label: 'Insert DataHub Transform Template',
        })
      )
    })

    it('should configure keyboard shortcut for template insertion', () => {
      addDataHubActionsToEditor(mockEditor as unknown as editor.IStandaloneCodeEditor, mockMonaco)

      expect(mockEditor.addAction).toHaveBeenCalledWith(
        expect.objectContaining({
          keybindings: [2048 | 1024 | 39], // CtrlCmd + Shift + I
        })
      )
    })

    it('should add action to context menu', () => {
      addDataHubActionsToEditor(mockEditor as unknown as editor.IStandaloneCodeEditor, mockMonaco)

      expect(mockEditor.addAction).toHaveBeenCalledWith(
        expect.objectContaining({
          contextMenuGroupId: 'navigation',
          contextMenuOrder: 1.5,
        })
      )
    })

    it('should execute template insertion when action runs', () => {
      const mockModel = {
        getFullModelRange: vi.fn().mockReturnValue({ startLineNumber: 1, endLineNumber: 1 }),
      }
      const mockFormatAction = {
        run: vi.fn(),
      }

      mockEditor.getModel.mockReturnValue(mockModel)
      mockEditor.getAction.mockReturnValue(mockFormatAction)

      addDataHubActionsToEditor(mockEditor as unknown as editor.IStandaloneCodeEditor, mockMonaco)

      // Get the run function from the addAction call
      const addActionCall = (mockEditor.addAction as ReturnType<typeof vi.fn>).mock.calls[0][0]
      const runFunction = addActionCall.run

      // Execute the run function
      runFunction(mockEditor)

      // Verify template was inserted
      expect(mockEditor.getModel).toHaveBeenCalled()
      expect(mockModel.getFullModelRange).toHaveBeenCalled()
      expect(mockEditor.executeEdits).toHaveBeenCalledWith('datahub-template', [
        {
          range: { startLineNumber: 1, endLineNumber: 1 },
          text: '// Template code\nfunction transform() {}',
        },
      ])
      expect(mockEditor.getAction).toHaveBeenCalledWith('editor.action.formatDocument')
      expect(mockFormatAction.run).toHaveBeenCalled()
    })

    it('should handle missing model gracefully', () => {
      mockEditor.getModel.mockReturnValue(null)

      addDataHubActionsToEditor(mockEditor as unknown as editor.IStandaloneCodeEditor, mockMonaco)

      const addActionCall = (mockEditor.addAction as ReturnType<typeof vi.fn>).mock.calls[0][0]
      const runFunction = addActionCall.run

      // Should not throw when model is null
      expect(() => runFunction(mockEditor)).not.toThrow()
      expect(mockEditor.executeEdits).not.toHaveBeenCalled()
    })
  })

  describe('registerDataHubCodeActions', () => {
    it('should register code action provider for javascript', () => {
      registerDataHubCodeActions(mockMonaco)

      expect(mockMonaco.languages.registerCodeActionProvider).toHaveBeenCalledWith(
        'javascript',
        expect.objectContaining({
          provideCodeActions: expect.any(Function),
        })
      )
    })

    it('should provide template insertion action for empty editor', () => {
      registerDataHubCodeActions(mockMonaco)

      const providerCall = (mockMonaco.languages.registerCodeActionProvider as ReturnType<typeof vi.fn>).mock
        .calls[0][1]
      const provideCodeActions = providerCall.provideCodeActions

      const mockModel = {
        getValue: vi.fn().mockReturnValue(''),
        uri: 'file:///test.js',
        getFullModelRange: vi.fn().mockReturnValue({ startLineNumber: 1, endLineNumber: 1 }),
        getVersionId: vi.fn().mockReturnValue(1),
      }

      const result = provideCodeActions(mockModel)

      expect(result.actions).toHaveLength(1)
      expect(result.actions[0].title).toBe('Insert DataHub Transform Template')
      expect(result.actions[0].kind).toBe('quickfix')
      expect(result.actions[0].isPreferred).toBe(true)
    })

    it('should not provide actions for non-empty editor', () => {
      registerDataHubCodeActions(mockMonaco)

      const providerCall = (mockMonaco.languages.registerCodeActionProvider as ReturnType<typeof vi.fn>).mock
        .calls[0][1]
      const provideCodeActions = providerCall.provideCodeActions

      const mockModel = {
        getValue: vi.fn().mockReturnValue('some existing code'),
        uri: 'file:///test.js',
      }

      const result = provideCodeActions(mockModel)

      expect(result.actions).toHaveLength(0)
    })

    it('should include template text in edit', () => {
      registerDataHubCodeActions(mockMonaco)

      const providerCall = (mockMonaco.languages.registerCodeActionProvider as ReturnType<typeof vi.fn>).mock
        .calls[0][1]
      const provideCodeActions = providerCall.provideCodeActions

      const mockModel = {
        getValue: vi.fn().mockReturnValue('   '), // Whitespace only counts as empty
        uri: 'file:///test.js',
        getFullModelRange: vi.fn().mockReturnValue({ startLineNumber: 1, endLineNumber: 1 }),
        getVersionId: vi.fn().mockReturnValue(1),
      }

      const result = provideCodeActions(mockModel)

      expect(result.actions[0].edit?.edits[0].textEdit.text).toBe('// Template code\nfunction transform() {}')
    })
  })

  describe('configureDataHubFeatures', () => {
    it('should configure DataHub features without errors', () => {
      expect(() => configureDataHubFeatures(mockMonaco)).not.toThrow()
      expect(mockMonaco.languages.registerCodeActionProvider).toHaveBeenCalledWith('javascript', expect.any(Object))
    })
  })
})
