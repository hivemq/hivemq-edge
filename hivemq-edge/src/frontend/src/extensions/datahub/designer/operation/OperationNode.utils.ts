import { getConnectedEdges, getIncomers, Node } from 'reactflow'

import { PolicyOperation, Schema, Script } from '@/api/__generated__'
import {
  DataHubNodeType,
  DataPolicyData,
  DryRunResults,
  FunctionData,
  OperationData,
  SchemaData,
  TransitionData,
  WorkspaceState,
} from '@datahub/types.ts'
import { checkValidityJSScript } from '@datahub/designer/script/FunctionNode.utils.ts'
import { checkValiditySchema } from '@datahub/designer/schema/SchemaNode.utils.ts'
import { PolicyCheckErrors } from '@datahub/designer/validation.errors.ts'

export function checkValidityTransformFunction(
  operationNode: Node<OperationData>,
  store: WorkspaceState
): DryRunResults<PolicyOperation, Script | Schema> {
  const { nodes, edges } = store

  if (!operationNode.data.functionId || !operationNode.data.formData) {
    return {
      node: operationNode,
      error: PolicyCheckErrors.notConfigured(operationNode, 'functionId, formData'),
    }
  }

  ///////// Check the function handle
  const functions = getIncomers(operationNode, nodes, edges).filter(
    (node) => node.type === DataHubNodeType.FUNCTION
  ) as Node<FunctionData>[]

  if (!functions.length) {
    return {
      node: operationNode,
      error: PolicyCheckErrors.notConnected(DataHubNodeType.FUNCTION, operationNode),
    }
  }

  ///////// Check the serializers
  const serialisers = getIncomers(operationNode, nodes, edges).filter(
    (node) => node.type === DataHubNodeType.SCHEMA
  ) as Node<SchemaData>[]

  const connectedEdges = getConnectedEdges([...serialisers], edges).filter(
    (e) => e.targetHandle === OperationData.Handle.SERIALISER || e.targetHandle === OperationData.Handle.DESERIALISER
  )
  const serial = connectedEdges.filter((edge) => edge.targetHandle === OperationData.Handle.SERIALISER)
  const deSerial = connectedEdges.filter((edge) => edge.targetHandle === OperationData.Handle.DESERIALISER)

  if (serial.length !== 1) {
    return {
      node: operationNode,
      error: PolicyCheckErrors.notConnected(DataHubNodeType.SCHEMA, operationNode, OperationData.Handle.SERIALISER),
    }
  }

  if (deSerial.length !== 1) {
    return {
      node: operationNode,
      error: PolicyCheckErrors.notConnected(DataHubNodeType.SCHEMA, operationNode, OperationData.Handle.SERIALISER),
    }
  }

  ///////// Check the resources
  const scriptNodes = functions.map((e) => checkValidityJSScript(e))
  const schemaNodes = serialisers.map((e) => checkValiditySchema(e))

  // TODO[NVL] if valid, needs to be broken down into three pipeline operation. with reference to the appropriate resource
  const operation: PolicyOperation = {
    functionId: operationNode.data.functionId,
    arguments: operationNode.data.formData,
    id: operationNode.id,
  }
  return { data: operation, node: operationNode, resources: [...scriptNodes, ...schemaNodes] }
}

export function checkValidityPipeline(
  source: Node<DataPolicyData> | Node<TransitionData>,
  handle: DataPolicyData.Handle | TransitionData.Handle,
  store: WorkspaceState
): DryRunResults<PolicyOperation>[] {
  const { nodes, edges } = store

  const getNextNode = (node: Node | undefined): Node | undefined => {
    if (node) {
      const outEdge = edges.find((edge) => edge.source === node.id)
      if (outEdge) {
        const nextNode = nodes.find((node) => node.id === outEdge.target)
        if (nextNode) return nextNode
      }
    }
    return undefined
  }

  const [outboundEdge] = edges.filter((edge) => edge.source === source.id && edge.sourceHandle === handle)
  if (!outboundEdge) {
    return []
  }

  const pipeline: Node[] = []
  let nextNode = nodes.find((node) => node.id === outboundEdge.target)
  while (nextNode) {
    pipeline.push(nextNode)
    nextNode = getNextNode(nextNode)
  }

  return pipeline.map((node) => {
    if (!node.data.functionId) {
      return {
        node: node,
        error: PolicyCheckErrors.notConfigured(node, 'functionId'),
      }
    }

    if (node.data.functionId === 'DataHub.transform') {
      return checkValidityTransformFunction(node, store)
    }

    // TODO[NVL] Serialisers need to be dealt with

    const operation: PolicyOperation = {
      functionId: node.data.functionId,
      arguments: node.data.formData,
      id: node.id,
    }
    return { node: node, data: operation }
  })
}
