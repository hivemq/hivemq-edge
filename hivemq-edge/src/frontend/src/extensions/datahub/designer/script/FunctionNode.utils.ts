import { Node } from 'reactflow'
import { DryRunResults, FunctionData } from '@datahub/types.ts'
import { Script } from '@/api/__generated__'
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
    source: scriptNode.data.sourceCode as string,
    version: scriptNode.data.version,
  }
  return { data: script, node: scriptNode }
}
