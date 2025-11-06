import type { MonacoInstance } from '../types'
import type { editor, languages } from 'monaco-editor'

/**
 * DataHub Transform boilerplate template
 */
const DATAHUB_TRANSFORM_TEMPLATE = `/**
 * @param {InitContext} initContext - Context providing setup methods
 */
function init(initContext) {
  // Create branches for message routing (optional)
  // const errorBranch = initContext.addBranch('error-handling');

  // Create client connection state (optional)
  // const messageCount = initContext.addClientConnectionState('count', 0);
}

/**
 * @param {Publish} publish - The MQTT PUBLISH packet to transform
 * @param {TransformContext} context - Transformation context
 * @returns {Publish|null} The transformed message, or null to drop it
 */
function transform(publish, context) {
  // Example: Add timestamp to payload
  // publish.payload.timestamp = Date.now();

  // Example: Modify topic
  // publish.topic = 'processed/' + publish.topic;

  // Example: Add user property (MQTT 5)
  // publish.userProperties.push({
  //   name: 'processedBy',
  //   value: 'datahub-transform'
  // });

  return publish;
}
`

/**
 * Register custom Monaco commands/actions for DataHub
 * Note: Editor-specific actions are registered in addDataHubActionsToEditor
 */
export const registerDataHubCommands = () => {
  // This is called during Monaco initialization
  // Editor-specific actions are added when each editor instance mounts
  console.log('[DataHub Commands] Command registration complete')
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
      console.log('[DataHub Commands] Template insertion command triggered!')

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

      console.log('[DataHub Commands] Template inserted - replaced entire content')

      // Format the document
      ed.getAction('editor.action.formatDocument')?.run()
    },
  })

  console.log('[DataHub Commands] Editor actions added')
}

/**
 * Register code action provider for DataHub JavaScript
 * Suggests inserting template when editor is empty
 */
export const registerDataHubCodeActions = (monaco: MonacoInstance) => {
  monaco.languages.registerCodeActionProvider('javascript', {
    provideCodeActions: (model) => {
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
