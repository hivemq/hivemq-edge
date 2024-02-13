import { getConnectedEdges, getIncomers, Node } from 'reactflow'
import {
  DataHubNodeType,
  DryRunResults,
  FunctionData,
  OperationData,
  TopicFilterData,
  WorkspaceState,
} from '@datahub/types.ts'
import { PolicyOperation, Script } from '@/api/__generated__'
import { checkValidityJSScript } from '@datahub/designer/script/FunctionNode.utils.ts'

export function checkValidityTransformFunction(
  operationNode: Node<OperationData>,
  store: WorkspaceState
): DryRunResults<PolicyOperation, Script> {
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
  ) as Node<TopicFilterData>[]

  const hh = getConnectedEdges([...serialisers], edges).filter(
    (e) => e.targetHandle === OperationData.Handle.SERIALISER || e.targetHandle === OperationData.Handle.DESERIALISER
  )
  const serial = hh.filter((e) => e.targetHandle === OperationData.Handle.SERIALISER)
  const deSerial = hh.filter((e) => e.targetHandle === OperationData.Handle.DESERIALISER)

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
  const scriptNodes = functions.map((e) => {
    return checkValidityJSScript(e)
  })

  const operation: PolicyOperation = {
    functionId: operationNode.data.functionId,
    arguments: operationNode.data.formData,
    id: operationNode.id,
  }
  return { data: operation, node: operationNode, resources: scriptNodes }
}
