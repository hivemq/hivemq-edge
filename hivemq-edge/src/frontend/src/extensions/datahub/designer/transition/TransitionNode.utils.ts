import { getOutgoers, Node, NodeAddChange, XYPosition } from 'reactflow'

import {
  BehaviorPolicy,
  BehaviorPolicyOnEvent,
  BehaviorPolicyOnTransition,
  PolicyOperation,
  Schema,
  Script,
} from '@/api/__generated__'
import i18n from '@/config/i18n.config.ts'

import {
  BehaviorPolicyData,
  BehaviorPolicyType,
  DataHubNodeType,
  DryRunResults,
  StateType,
  TransitionData,
  TransitionType,
  WorkspaceAction,
  WorkspaceState,
} from '@datahub/types.ts'
import { PolicyCheckErrors } from '@datahub/designer/validation.errors.ts'

import { checkValidityPipeline, loadBehaviorPolicyPipelines } from '@datahub/designer/operation/OperationNode.utils.ts'
import { getNodeId, isTransitionNodeType } from '@datahub/utils/node.utils.ts'
import { enumFromStringValue } from '@/utils/types.utils.ts'
import { CANVAS_POSITION } from '@datahub/designer/checks.utils.ts'

export function checkValidityTransitions(
  behaviorPolicyData: Node<BehaviorPolicyData>,
  store: WorkspaceState
): {
  behaviorPolicyTransitions: DryRunResults<BehaviorPolicyOnTransition>[]
  pipelines?: DryRunResults<PolicyOperation>[]
} {
  const { nodes, edges } = store

  const transitions = getOutgoers(behaviorPolicyData, nodes, edges).filter(isTransitionNodeType)

  if (!transitions.length) {
    return {
      behaviorPolicyTransitions: [
        {
          node: behaviorPolicyData,
          error: PolicyCheckErrors.notConnected(DataHubNodeType.TRANSITION, behaviorPolicyData),
        },
      ],
    }
  }

  const pipelines: DryRunResults<PolicyOperation>[] = []

  const behaviorPolicyTransitions = transitions.map<DryRunResults<BehaviorPolicyOnTransition>>((transition) => {
    if (!transition.data.event || !transition.data.from || !transition.data.to) {
      return {
        node: transition,
        error: PolicyCheckErrors.notConfigured(transition, 'event, from, to'),
      }
    }

    const pipeline = checkValidityPipeline(transition, TransitionData.Handle.OPERATION, store)
    pipelines.push(...pipeline)

    // TODO[19240] Making an assumption: we can incorporate the "correct" parts of the pipeline
    const validPipeline = pipeline.filter((operation) => !!operation.data)
    const policyEvents: BehaviorPolicyOnEvent = {
      pipeline: validPipeline.map((operation) => operation.data as PolicyOperation),
    }

    return {
      node: transition,
      data: {
        fromState: transition.data.from,
        toState: transition.data.to,
        [transition.data.event]: policyEvents,
      },
    }
  })

  return { behaviorPolicyTransitions, pipelines }
}

export const getActiveTransition = (transition: BehaviorPolicyOnTransition) => {
  if (transition['Event.OnAny']) return 'Event.OnAny'
  if (transition['Connection.OnDisconnect']) return 'Connection.OnDisconnect'
  if (transition['Mqtt.OnInboundConnect']) return 'Mqtt.OnInboundConnect'
  if (transition['Mqtt.OnInboundDisconnect']) return 'Mqtt.OnInboundDisconnect'
  if (transition['Mqtt.OnInboundPublish']) return 'Mqtt.OnInboundPublish'
  if (transition['Mqtt.OnInboundSubscribe']) return 'Mqtt.OnInboundSubscribe'
  // TODO[DATAHUB] is an unknown or undefined transition allowed?
  return undefined
}

const extractEventStates = (
  behaviorPolicyTransition: BehaviorPolicyOnTransition
): Pick<TransitionData, 'event' | 'from' | 'to'> => {
  // return { event: TransitionType.ON_INBOUND_DISCONNECT, from: StateType.Publishing, to: StateType.Violated }
  return {
    event: enumFromStringValue(TransitionType, getActiveTransition(behaviorPolicyTransition) || ''),
    from: enumFromStringValue(StateType, behaviorPolicyTransition.fromState),
    to: enumFromStringValue(StateType, behaviorPolicyTransition.toState),
  }
}

export const loadTransitions = (
  behaviorPolicy: BehaviorPolicy,
  schemas: Schema[],
  scripts: Script[],
  store: WorkspaceState & WorkspaceAction
) => {
  const { onNodesChange, onConnect } = store
  const BehaviorPolicyNode = store.nodes.find((n) => n.id === behaviorPolicy.id)
  if (!BehaviorPolicyNode)
    throw new Error(
      i18n.t('datahub:error.loading.connection.notFound', { type: DataHubNodeType.BEHAVIOR_POLICY }) as string
    )
  const model = enumFromStringValue(BehaviorPolicyType, behaviorPolicy.behavior.id)
  if (!model) throw new Error(i18n.t('datahub:error.loading.behavior.noModel') as string)

  const position: XYPosition = {
    x: BehaviorPolicyNode.position.x + CANVAS_POSITION.Transition.x,
    y: BehaviorPolicyNode.position.y + CANVAS_POSITION.Transition.y,
  }

  const shiftBottom = () => {
    position.y += CANVAS_POSITION.Transition.y
    return position
  }

  for (const behaviorPolicyTransition of behaviorPolicy.onTransitions || []) {
    const transitionNode: Node<TransitionData> = {
      id: getNodeId(),
      type: DataHubNodeType.TRANSITION,
      position: { ...shiftBottom() },
      data: {
        model: model,
        ...extractEventStates(behaviorPolicyTransition),
      },
    }

    onNodesChange([{ item: transitionNode, type: 'add' } as NodeAddChange])
    onConnect({ source: behaviorPolicy.id, target: transitionNode.id, sourceHandle: null, targetHandle: null })
    loadBehaviorPolicyPipelines(behaviorPolicy, transitionNode, schemas, scripts, store)
  }
}
