import debug from 'debug'
import type { MonacoInstance } from '../types'
import type { editor, languages } from 'monaco-editor'
import transformTemplate from '../__test-utils__/transform-template.js?raw'

const debugLogger = debug('DataHub:monaco:js')

/**
 * DataHub Transform boilerplate template
 * Loaded from: src/extensions/datahub/components/forms/monaco/templates/transform-template.js
 */
const DATAHUB_TRANSFORM_TEMPLATE = transformTemplate

/**
 * Register custom Monaco commands/actions for DataHub
 * Note: Editor-specific actions are registered in addDataHubActionsToEditor
 */
export const registerDataHubCommands = () => {
  // This is called during Monaco initialization
  // Editor-specific actions are added when each editor instance mounts
  debugLogger('[DataHub Commands] Command registration complete')
}

/**
 * Add DataHub actions to a specific editor instance
 */
export const addDataHubActionsToEditor = (editor: editor.IStandaloneCodeEditor, monaco: MonacoInstance) => {
  // Add action to insert transform template
  editor.addAction({
    id: 'datahub.insertTransformTemplate',
    label: 'Insert DataHub Transform Template',
    keybindings: [monaco.KeyMod.CtrlCmd | monaco.KeyMod.Shift | monaco.KeyCode.KeyI],
    contextMenuGroupId: 'navigation',
    contextMenuOrder: 1.5,
    run: (ed) => {
      debugLogger('Template insertion command triggered!')

      const model = ed.getModel()
      if (!model) {
        return
      }

      // Always replace entire content with template
      ed.executeEdits('datahub-template', [
        {
          range: model.getFullModelRange(),
          text: DATAHUB_TRANSFORM_TEMPLATE,
        },
      ])

      debugLogger('Template inserted - replaced entire content')

      // Format the document
      ed.getAction('editor.action.formatDocument')?.run()
    },
  })

  debugLogger('Editor actions added')
}

/**
 * Register code action provider for DataHub JavaScript
 * Suggests inserting template when editor is empty
 */
export const registerDataHubCodeActions = (monaco: MonacoInstance) => {
  monaco.languages.registerCodeActionProvider('javascript', {
    provideCodeActions: (model: editor.ITextModel) => {
      const isEmpty = model.getValue().trim() === ''

      if (!isEmpty) {
        return {
          actions: [],
          dispose: () => {},
        }
      }

      // Suggest inserting template when editor is empty
      const action: languages.CodeAction = {
        title: 'Insert DataHub Transform Template',
        kind: 'quickfix',
        diagnostics: [],
        isPreferred: true,
        edit: {
          edits: [
            {
              resource: model.uri,
              textEdit: {
                range: model.getFullModelRange(),
                text: DATAHUB_TRANSFORM_TEMPLATE,
              },
              versionId: model.getVersionId(),
            },
          ],
        },
      }

      return {
        actions: [action],
        dispose: () => {},
      }
    },
  })
}

/**
 * Configure all DataHub-specific Monaco features
 */
export const configureDataHubFeatures = (monaco: MonacoInstance) => {
  registerDataHubCommands()
  registerDataHubCodeActions(monaco)
}
