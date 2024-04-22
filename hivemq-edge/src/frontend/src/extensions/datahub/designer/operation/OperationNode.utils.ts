import { Connection, getConnectedEdges, getIncomers, Node, NodeAddChange, XYPosition } from 'reactflow'

import i18n from '@/config/i18n.config.ts'

import { BehaviorPolicy, DataPolicy, PolicyOperation, Schema, SchemaReference, Script } from '@/api/__generated__'
import {
  DataHubNodeType,
  DataPolicyData,
  DryRunResults,
  FunctionData,
  OperationData,
  PolicyOperationArguments,
  ResourceStatus,
  TransitionData,
  WorkspaceAction,
  WorkspaceState,
} from '@datahub/types.ts'
import {
  checkValidityJSScript,
  formatScriptName,
  loadScripts,
  parseScriptName,
} from '@datahub/designer/script/FunctionNode.utils.ts'
import { checkValiditySchema, loadSchema } from '@datahub/designer/schema/SchemaNode.utils.ts'
import { PolicyCheckErrors } from '@datahub/designer/validation.errors.ts'
import { isFunctionNodeType, isSchemaNodeType } from '@datahub/utils/node.utils.ts'
import { getActiveTransition } from '@datahub/designer/transition/TransitionNode.utils.ts'
import { CANVAS_POSITION } from '@datahub/designer/checks.utils.ts'

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
    (edge) =>
      edge.targetHandle === OperationData.Handle.SERIALISER || edge.targetHandle === OperationData.Handle.DESERIALISER
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

  ///////// Check the resources
  const scriptNodes = functions.map((node) => checkValidityJSScript(node))
  const schemaNodes = serialisers.map((node) => checkValiditySchema(node))

  if (!scriptNodes.length) {
    return [
      {
        node: operationNode,
        error: PolicyCheckErrors.notConfigured(operationNode, 'script name'),
      },
    ]
  }

  const { transform } = operationNode.data.formData as unknown as OperationData.DataHubTransformType
  const defaultOrder = transform.length ? transform : scriptNodes.map((e) => e.data?.id)

  const allTransformScripts: DryRunResults<PolicyOperation, Script>[] = []
  for (const scriptId of defaultOrder) {
    const script = scriptNodes.find((e) => e.data?.id === scriptId)
    if (script) {
      const scriptName = script.node as Node<FunctionData>

      const operation: PolicyOperation = {
        functionId: formatScriptName(scriptName),
        arguments: {},
        // TODO[19466] Id should be user-facing; Need to fix before merging!
        id: scriptName.id,
      }
      allTransformScripts.push({ data: operation, node: operationNode })
    }
  }

  const sourceDeserial = serialisers.find((node) => node.id === deserial.source)
  if (!sourceDeserial) {
    return [
      {
        node: operationNode,
        error: PolicyCheckErrors.notConnected(DataHubNodeType.SCHEMA, operationNode, OperationData.Handle.DESERIALISER),
      },
    ]
  }
  const deserializer: PolicyOperation = {
    functionId: OperationData.Function.SERDES_DESERIALIZE,
    arguments: {
      schemaId: sourceDeserial.data.name,
      schemaVersion:
        sourceDeserial.data.version === ResourceStatus.DRAFT || sourceDeserial.data.version === ResourceStatus.MODIFIED
          ? 'latest'
          : sourceDeserial.data.version.toString(),
    } as PolicyOperationArguments,
    id: `${operationNode.id}-deserializer`,
  }

  const sourceSerial = serialisers.find((node) => node.id === serial.source)
  if (!sourceSerial) {
    return [
      {
        node: operationNode,
        error: PolicyCheckErrors.notConnected(DataHubNodeType.SCHEMA, operationNode, OperationData.Handle.SERIALISER),
      },
    ]
  }

  const serializer: PolicyOperation = {
    functionId: OperationData.Function.SERDES_SERIALIZE,
    arguments: {
      schemaId: sourceSerial.data.name,
      schemaVersion:
        sourceSerial.data.version === ResourceStatus.DRAFT || sourceSerial.data.version === ResourceStatus.MODIFIED
          ? 'latest'
          : sourceSerial.data.version.toString(),
    } as PolicyOperationArguments,
    id: `${operationNode.id}-serializer`,
  }

  // The resources are added tp the last item for convenience
  return [
    { data: deserializer, node: operationNode },
    ...allTransformScripts,
    { data: serializer, node: operationNode, resources: [...scriptNodes, ...schemaNodes] },
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
    if (!activeTransition)
      throw new Error(i18n.t('datahub:error.loading.operation.noTransition', { source: activeTransition }) as string)

    const transitionOnEvent = transition[activeTransition]
    if (!transitionOnEvent)
      throw new Error(i18n.t('datahub:error.loading.operation.noTransition', { source: activeTransition }) as string)

    loadPipeline(transitionNode, transitionOnEvent.pipeline, null, schemas, scripts, store)
  }
}

export const loadDataPolicyPipelines = (
  policy: DataPolicy,
  schemas: Schema[],
  scripts: Script[],
  store: WorkspaceState & WorkspaceAction
) => {
  const dataNode = store.nodes.find((node) => node.id === policy.id)
  if (!dataNode)
    throw new Error(
      i18n.t('datahub:error.loading.connection.notFound', { type: DataHubNodeType.DATA_POLICY }) as string
    )

  if (policy.onSuccess && policy.onSuccess.pipeline)
    loadPipeline(dataNode, policy.onSuccess.pipeline, DataPolicyData.Handle.ON_SUCCESS, schemas, scripts, store)
  if (policy.onFailure && policy.onFailure.pipeline)
    loadPipeline(dataNode, policy.onFailure.pipeline, DataPolicyData.Handle.ON_ERROR, schemas, scripts, store)
}

export const loadPipeline = (
  parentNode: Node,
  pipeline: Array<PolicyOperation>,
  handle: DataPolicyData.Handle | null,
  schemas: Schema[],
  scripts: Script[],
  store: WorkspaceState & WorkspaceAction
) => {
  const { onAddNodes, onConnect } = store
  if (!parentNode)
    throw new Error(i18n.t('datahub:error.loading.schema.unknown', { type: DataHubNodeType.DATA_POLICY }) as string)

  const delta =
    handle === DataPolicyData.Handle.ON_ERROR ? CANVAS_POSITION.OperationError : CANVAS_POSITION.OperationSuccess
  const position: XYPosition = {
    x: parentNode.position.x + delta.x,
    y: parentNode.position.y + delta.y,
  }

  const shiftPositionRight = () => {
    position.x += delta.x
    return position
  }

  let connect: Connection = {
    source: parentNode.id,
    target: null,
    sourceHandle: handle,
    targetHandle: null,
  }
  let operationNode: Node<OperationData> | undefined | PolicyOperation[] = undefined
  for (const policyOperation of pipeline) {
    switch (true) {
      case OperationData.Function.SERDES_DESERIALIZE === policyOperation.functionId:
        if (operationNode) throw new Error(i18n.t('datahub:error.loading.operation.unknown') as string)
        operationNode = [policyOperation]
        break
      case policyOperation.functionId.startsWith('fn:'):
        if (!Array.isArray(operationNode)) throw new Error(i18n.t('datahub:error.loading.operation.unknown') as string)
        operationNode?.push(policyOperation)
        break
      case OperationData.Function.SERDES_SERIALIZE === policyOperation.functionId:
        {
          if (!Array.isArray(operationNode))
            throw new Error(i18n.t('datahub:error.loading.operation.unknown') as string)
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
              formData: { transform: functions.map((operation) => parseScriptName(operation)) },
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
        if (operationNode) throw new Error(i18n.t('datahub:error.loading.operation.unknown') as string)
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

    if (!operationNode) throw new Error(i18n.t('datahub:error.loading.operation.unknown') as string)
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
