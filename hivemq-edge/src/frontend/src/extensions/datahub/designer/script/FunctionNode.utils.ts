import { Node } from 'reactflow'
import { DryRunResults, FunctionData } from '@datahub/types.ts'
import { Script } from '@/api/__generated__'

export function checkValidityJSScript(scriptNode: Node<FunctionData>): DryRunResults<Script> {
  if (!scriptNode.data.name || !scriptNode.data.version) {
    return {
      node: scriptNode,
      error: {
        title: scriptNode.type as string,
        status: 404,
        detail: 'The Javascript function is not properly described. NAme and version are missing',
        type: 'datahub.notDefined',
        id: scriptNode.id,
      },
    }
  }

  const script: Script = {
    id: scriptNode.data.name,
    functionType: Script.functionType.TRANSFORMATION,
    source: scriptNode.data.sourceCode as string,
    // @ts-ignore TODO[NVL] Need to fix before merging!
    version: scriptNode.data.version,
  }
  return { data: script, node: scriptNode }
}
