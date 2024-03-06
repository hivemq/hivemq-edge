import { Connection, getConnectedEdges, getIncomers, Node, NodeAddChange, XYPosition } from 'reactflow'

import { BehaviorPolicy, DataPolicy, PolicyOperation, Schema, SchemaReference, Script } from '@/api/__generated__'
import {
  DataHubNodeType,
  DataPolicyData,
  DryRunResults,
  FunctionData,
  OperationData,
  PolicyOperationArguments,
  TransitionData,
  WorkspaceAction,
  WorkspaceState,
} from '@datahub/types.ts'
import { checkValidityJSScript, loadScripts } from '@datahub/designer/script/FunctionNode.utils.ts'
import { checkValiditySchema, loadSchema } from '@datahub/designer/schema/SchemaNode.utils.ts'
import { PolicyCheckErrors } from '@datahub/designer/validation.errors.ts'
import { isFunctionNodeType, isSchemaNodeType } from '@datahub/utils/node.utils.ts'
import { getActiveTransition } from '@datahub/designer/transition/TransitionNode.utils.ts'

export function checkValidityTransformFunction(
  operationNode: Node<OperationData>,
  store: WorkspaceState
): DryRunResults<PolicyOperation, Script | Schema>[] {
  const { nodes, edges } = store

  if (!operationNode.data.functionId || !operationNode.data.formData) {
    return [
      {
        node: operationNode,
        error: PolicyCheckErrors.notConfigured(operationNode, 'functionId, formData'),
      },
    ]
  }

  ///////// Check the function handle
  const functions = getIncomers(operationNode, nodes, edges).filter(isFunctionNodeType)

  if (!functions.length) {
    return [
      {
        node: operationNode,
        error: PolicyCheckErrors.notConnected(DataHubNodeType.FUNCTION, operationNode),
      },
    ]
  }

  ///////// Check the serializers
  const serialisers = getIncomers(operationNode, nodes, edges).filter(isSchemaNodeType)

  const connectedEdges = getConnectedEdges([...serialisers], edges).filter(
    (e) => e.targetHandle === OperationData.Handle.SERIALISER || e.targetHandle === OperationData.Handle.DESERIALISER
  )
  const [serial, ...restSerial] = connectedEdges.filter((edge) => edge.targetHandle === OperationData.Handle.SERIALISER)
  const [deserial, ...restDeserial] = connectedEdges.filter(
    (edge) => edge.targetHandle === OperationData.Handle.DESERIALISER
  )

  if (serial === undefined || restSerial.length !== 0) {
    return [
      {
        node: operationNode,
        error: PolicyCheckErrors.notConnected(DataHubNodeType.SCHEMA, operationNode, OperationData.Handle.SERIALISER),
      },
    ]
  }

  if (deserial === undefined || restDeserial.length !== 0) {
    return [
      {
        node: operationNode,
        error: PolicyCheckErrors.notConnected(DataHubNodeType.SCHEMA, operationNode, OperationData.Handle.DESERIALISER),
      },
    ]
  }

  // TODO[19240] Should serial and deserial be different ?

  ///////// Check the resources
  const scriptNodes = functions.map((node) => checkValidityJSScript(node))
  const schemaNodes = serialisers.map((node) => checkValiditySchema(node))

  const scriptName = scriptNodes[0].node as Node<FunctionData>
  if (!scriptName) {
    return [
      {
        node: operationNode,
        error: PolicyCheckErrors.notConfigured(operationNode, 'script name'),
      },
    ]
  }

  // TODO[19497] This should not have to happen on the client side!
  const formattedScriptName = (functionNode: Node<FunctionData>): string => {
    return `fn:${functionNode.data.name}:latest`
  }

  const deserializer: PolicyOperation = {
    // TODO[19240] Should not be hardcoded and should be Typescript-ed
    functionId: 'Serdes.deserialize',
    arguments: {
      // TODO[19466] Id should come from the node's data when fixed; Need to fix before merging!
      schemaId: serial.source,
      schemaVersion: 'latest',
    } as PolicyOperationArguments,
    id: `${operationNode.id}-deserializer`,
  }

  // TODO[19497] there should be a list of functions
  const operation: PolicyOperation = {
    functionId: formattedScriptName(scriptName),
    arguments: operationNode.data.formData,
    // TODO[19466] Id should be user-facing; Need to fix before merging!
    id: operationNode.id,
  }

  const serializer: PolicyOperation = {
    // TODO[19240] Should not be hardcoded and should be Typescript-ed
    functionId: 'Serdes.serialize',
    arguments: {
      // TODO[19466] Id should come from the node's data when fixed; Need to fix before merging!
      schemaId: deserial.source,
      schemaVersion: 'latest',
    } as PolicyOperationArguments,
    id: `${operationNode.id}-serializer`,
  }
  return [
    { data: deserializer, node: operationNode },
    // TODO[NVL] Technically, resources should be associated with the serialiser/deserialiser
    { data: operation, node: operationNode, resources: [...scriptNodes, ...schemaNodes] },
    { data: serializer, node: operationNode },
  ]
}

export const processOperations =
  (store: WorkspaceState) => (acc: DryRunResults<PolicyOperation, never>[], node: Node) => {
    if (!node.data.functionId) {
      acc.push({
        node: node,
        error: PolicyCheckErrors.notConfigured(node, 'functionId'),
      })
    } else if (node.data.functionId === 'DataHub.transform') {
      const transformResults = checkValidityTransformFunction(node, store)
      acc.push(...transformResults)
    } else {
      const operation: PolicyOperation = {
        functionId: node.data.functionId,
        arguments: node.data.formData || {},
        // TODO[19466] Id should be user-facing; Need to fix before merging!
        id: node.id,
      }
      acc.push({ node: node, data: operation })
    }
    return acc
  }

export function checkValidityPipeline(
  source: Node<DataPolicyData> | Node<TransitionData>,
  handle: DataPolicyData.Handle | TransitionData.Handle,
  store: WorkspaceState
): DryRunResults<PolicyOperation>[] {
  const { nodes, edges } = store

  /* istanbul ignore next -- @preserve */
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

  return pipeline.reduce<DryRunResults<PolicyOperation>[]>(processOperations(store), [])
}

export const loadBehaviorPolicyPipelines = (
  behaviorPolicy: BehaviorPolicy,
  transitionNode: Node,
  schemas: Schema[],
  scripts: Script[],
  store: WorkspaceState & WorkspaceAction
) => {
  for (const transition of behaviorPolicy.onTransitions || []) {
    const activeTransition = getActiveTransition(transition)
    if (!activeTransition) throw new Error("cannot create the transition pipeline's nodes")

    const transitionOnEvent = transition[activeTransition]
    if (!transitionOnEvent) throw new Error("cannot create the transition pipeline's nodes")

    loadPipeline(transitionNode, transitionOnEvent.pipeline, null, schemas, scripts, store)
  }
}

export const loadDataPolicyPipelines = (
  policy: DataPolicy,
  schemas: Schema[],
  scripts: Script[],
  store: WorkspaceState & WorkspaceAction
) => {
  if (policy.onSuccess && policy.onSuccess.pipeline)
    loadPipeline(policy, policy.onSuccess.pipeline, DataPolicyData.Handle.ON_SUCCESS, schemas, scripts, store)
  if (policy.onFailure && policy.onFailure.pipeline)
    loadPipeline(policy, policy.onFailure.pipeline, DataPolicyData.Handle.ON_ERROR, schemas, scripts, store)
}

export const loadPipeline = (
  policy: DataPolicy,
  pipeline: Array<PolicyOperation>,
  handle: DataPolicyData.Handle,
  schemas: Schema[],
  scripts: Script[],
  store: WorkspaceState & WorkspaceAction
) => {
  const { onAddNodes, onConnect } = store
  const dataNode = store.nodes.find((n) => n.id === policy.id)
  if (!dataNode) throw new Error('cannot find the data policy node')

  const position: XYPosition = {
    x: dataNode.position.x,
    y: dataNode.position.y + (handle === DataPolicyData.Handle.ON_ERROR ? 100 : 0),
  }

  const shiftPositionRight = () => {
    position.x += 400
    return position
  }

  let connect: Connection = {
    source: dataNode.id,
    target: null,
    sourceHandle: handle,
    targetHandle: null,
  }
  let operationNode: Node<OperationData> | undefined | PolicyOperation[] = undefined
  for (const policyOperation of pipeline) {
    switch (true) {
      case OperationData.Function.SERDES_DESERIALIZE === policyOperation.functionId:
        if (operationNode) throw new Error('Something wrong with the operation')
        operationNode = [policyOperation]
        break
      case policyOperation.functionId.startsWith('fn:'):
        if (!Array.isArray(operationNode)) throw new Error('something wrong with the operation')
        operationNode?.push(policyOperation)
        break
      case OperationData.Function.SERDES_SERIALIZE === policyOperation.functionId:
        {
          if (!Array.isArray(operationNode)) throw new Error('something wrong with the operation')
          const [deserializer, ...functions] = operationNode as PolicyOperation[]

          operationNode = {
            id: policyOperation.id,
            type: DataHubNodeType.OPERATION,
            position: { ...shiftPositionRight() },
            data: {
              functionId: OperationData.Function.DATAHUB_TRANSFORM,
              metadata: {
                isTerminal: false,
                hasArguments: true,
              },
              formData: { transform: functions.map((e) => e.functionId) },
            },
          }

          loadSchema(
            operationNode,
            OperationData.Handle.DESERIALISER,
            -200,
            deserializer.arguments as SchemaReference,
            schemas,
            store
          )
          loadSchema(
            operationNode,
            OperationData.Handle.SERIALISER,
            200,
            policyOperation.arguments as SchemaReference,
            schemas,
            store
          )

          loadScripts(operationNode, functions, scripts, store)
        }
        break
      default:
        if (operationNode) throw new Error('something wrong with the operation')
        operationNode = {
          id: policyOperation.id,
          type: DataHubNodeType.OPERATION,
          position: { ...shiftPositionRight() },
          data: {
            functionId: policyOperation.functionId,
            formData: policyOperation.arguments,
          },
        }
    }

    if (!operationNode) throw new Error('something wrong with the operation')
    if (!Array.isArray(operationNode)) {
      onAddNodes([{ item: operationNode, type: 'add' } as NodeAddChange])
      onConnect({ ...connect, target: operationNode.id })
      connect = {
        source: operationNode.id,
        target: null,
        sourceHandle: null,
        targetHandle: null,
      }
      operationNode = undefined
    }
  }
}
