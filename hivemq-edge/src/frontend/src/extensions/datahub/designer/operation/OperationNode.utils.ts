import { getConnectedEdges, getIncomers, Node } from 'reactflow'
import {
  DataHubNodeType,
  DryRunResults,
  FunctionData,
  OperationData,
  SchemaData,
  WorkspaceState,
} from '@datahub/types.ts'
import { PolicyOperation, Schema, Script } from '@/api/__generated__'
import { checkValidityJSScript } from '@datahub/designer/script/FunctionNode.utils.ts'
import { checkValiditySchema } from '@datahub/designer/schema/SchemaNode.utils.ts'

export function checkValidityTransformFunction(
  operationNode: Node<OperationData>,
  store: WorkspaceState
): DryRunResults<PolicyOperation, Script | Schema> {
  const { nodes, edges } = store

  if (!operationNode.data.functionId || !operationNode.data.formData) {
    return {
      node: operationNode,
      error: {
        status: 404,
        title: operationNode.type as string,
        detail: 'The event has not been defined',
        type: 'datahub.notDefined',
        id: operationNode.id,
      },
    }
  }

  ///////// Check the function handle
  const functions = getIncomers(operationNode, nodes, edges).filter(
    (node) => node.type === DataHubNodeType.FUNCTION
  ) as Node<FunctionData>[]

  if (!functions.length) {
    return {
      node: operationNode,
      error: {
        title: operationNode.type as string,
        status: 404,
        detail: 'No function connected to the operation node',
        type: 'datahub.notConnected',
        id: operationNode.id,
      },
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
      error: {
        title: operationNode.type as string,
        status: 404,
        detail: 'No schema connected to the serialiser handle',
        type: 'datahub.notConnected',
        id: operationNode.id,
      },
    }
  }

  if (deSerial.length !== 1) {
    return {
      node: operationNode,
      error: {
        title: operationNode.type as string,
        status: 404,
        detail: 'No schema connected to the deserialiser handle',
        type: 'datahub.notConnected',
        id: operationNode.id,
      },
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
