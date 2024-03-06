import { Node, NodeAddChange } from 'reactflow'
import {
  DataHubNodeData,
  DataHubNodeType,
  DryRunResults,
  FunctionData,
  OperationData,
  WorkspaceAction,
  WorkspaceState,
} from '@datahub/types.ts'
import { PolicyOperation, Script } from '@/api/__generated__'
import { PolicyCheckErrors } from '@datahub/designer/validation.errors.ts'

export function checkValidityJSScript(scriptNode: Node<FunctionData>): DryRunResults<Script> {
  if (!scriptNode.data.name || !scriptNode.data.version || !scriptNode.data.sourceCode) {
    return {
      node: scriptNode,
      error: PolicyCheckErrors.notConfigured(scriptNode, 'name, version, sourceCode'),
    }
  }

  const script: Script = {
    // TODO[19466] Id should be user-facing; Need to fix before merging!
    id: scriptNode.data.name,
    functionType: Script.functionType.TRANSFORMATION,
    source: btoa(scriptNode.data.sourceCode),
    // version is generated by the backend
  }
  return { data: script, node: scriptNode }
}

export const loadScripts = (
  parentNode: Node<DataHubNodeData>,
  functions: PolicyOperation[],
  scripts: Script[],
  store: WorkspaceState & WorkspaceAction
) => {
  const { onAddNodes, onConnect } = store
  for (const fct of functions) {
    const [, functionName] = fct.functionId.split(':')
    const functionScript = scripts.find((e) => e.id === functionName)
    if (!functionScript) throw new Error('something wrong with the operation')

    const functionScriptNode: Node<FunctionData> = {
      id: functionScript.id,
      type: DataHubNodeType.FUNCTION,
      position: { x: parentNode.position.x, y: parentNode.position.y - 275 },
      data: {
        type: 'Javascript',
        name: functionScript.id,
        version: Number(functionScript.version),
        sourceCode: atob(functionScript.source),
      },
    }
    onAddNodes([{ item: functionScriptNode, type: 'add' } as NodeAddChange])
    onConnect({
      source: functionScriptNode.id,
      target: parentNode.id,
      sourceHandle: null,
      targetHandle: OperationData.Handle.FUNCTION,
    })
  }
}
