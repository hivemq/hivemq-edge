import { Edge, Node } from 'reactflow'
import { RJSFSchema } from '@rjsf/utils'

import { FunctionSpecs, WorkspaceState } from '@datahub/types.ts'
import { MOCK_OPERATION_SCHEMA } from '@datahub/designer/operation/OperationData.ts'

const getFunctions = (schema: RJSFSchema) => {
  if (!schema) return []

  const definitions = schema.definitions
  if (!definitions) return []

  // @ts-ignore TODO[NVL] A weird jsonschema structure to go through; better alternative?
  const functionNames = definitions.functionId?.properties?.functionId?.enum as string[]

  if (!definitions || !functionNames) return []

  const functions: FunctionSpecs[] = functionNames.map((functionName) => {
    const definition = definitions[functionName]
    if (!definition) return {} as FunctionSpecs

    // @ts-ignore
    const metadata: FunctionDefinition = definition.metadata
    const functionSpec: FunctionSpecs = {
      functionId: functionName,
      schema: definition as RJSFSchema,
      metadata,
    }
    return functionSpec
  })

  return functions || []
}

export const initialStore = (): WorkspaceState => {
  const nodes: Node[] = []
  const edges: Edge[] = []

  const functions = getFunctions(MOCK_OPERATION_SCHEMA.schema)

  return { nodes, edges, functions: functions }
}
