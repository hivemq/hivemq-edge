import type { Connection, Node, NodeAddChange, XYPosition } from '@xyflow/react'
import { getOutgoers } from '@xyflow/react'

import type {
  BehaviorPolicy,
  BehaviorPolicyOnEvent,
  BehaviorPolicyOnTransition,
  PolicyOperation,
  PolicySchema,
  Script,
} from '@/api/__generated__'
import { BehaviorPolicyTransitionEvent } from '@/api/__generated__'
import i18n from '@/config/i18n.config.ts'
import { enumFromStringValue } from '@/utils/types.utils.ts'

import type { BehaviorPolicyData, DryRunResults, FiniteStateMachineSchema, WorkspaceState } from '@datahub/types.ts'
import { BehaviorPolicyType, DataHubNodeType, StateType, TransitionData } from '@datahub/types.ts'
import { PolicyCheckErrors } from '@datahub/designer/validation.errors.ts'
import { checkValidityPipeline, loadBehaviorPolicyPipelines } from '@datahub/designer/operation/OperationNode.utils.ts'
import { CANVAS_POSITION } from '@datahub/designer/checks.utils.ts'
import { MOCK_BEHAVIOR_POLICY_SCHEMA } from '@datahub/designer/behavior_policy/BehaviorPolicySchema.ts'
import { getNodeId, isTransitionNodeType } from '@datahub/utils/node.utils.ts'

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

export const extractEventStates = (
  model: BehaviorPolicyType,
  behaviorPolicyTransition: BehaviorPolicyOnTransition
): TransitionData => {
  const definition = MOCK_BEHAVIOR_POLICY_SCHEMA.schema.definitions?.[model]
  const { metadata } = definition as FiniteStateMachineSchema
  const { states } = metadata

  const to = enumFromStringValue(StateType, behaviorPolicyTransition.toState)
  const endState = states.find((state) => state.name === to)

  return {
    model: model,
    event: enumFromStringValue(BehaviorPolicyTransitionEvent, getActiveTransition(behaviorPolicyTransition) || ''),
    from: enumFromStringValue(StateType, behaviorPolicyTransition.fromState),
    to,
    type: endState?.type,
  }
}

export const loadTransitions = (
  behaviorPolicy: BehaviorPolicy,
  schemas: PolicySchema[],
  scripts: Script[],
  behaviorPolicyNode: Node<BehaviorPolicyData>
) => {
  const model = enumFromStringValue(BehaviorPolicyType, behaviorPolicy.behavior.id)
  if (!model) throw new Error(i18n.t('datahub:error.loading.behavior.noModel') as string)

  const delta = ((Math.max(behaviorPolicy.onTransitions?.length || 0, 1) - 1) * CANVAS_POSITION.Transition.y) / 2
  const position: XYPosition = {
    x: behaviorPolicyNode.position.x + CANVAS_POSITION.Transition.x,
    y: behaviorPolicyNode.position.y - CANVAS_POSITION.Transition.y - delta,
  }

  const shiftBottom = () => {
    position.y += CANVAS_POSITION.Transition.y
    return position
  }

  const newNodes: (NodeAddChange | Connection)[] = []
  for (const behaviorPolicyTransition of behaviorPolicy.onTransitions || []) {
    const transitionNode: Node<TransitionData> = {
      id: getNodeId(DataHubNodeType.TRANSITION),
      type: DataHubNodeType.TRANSITION,
      position: { ...shiftBottom() },
      data: extractEventStates(model, behaviorPolicyTransition),
    }
    const pipelines = loadBehaviorPolicyPipelines(behaviorPolicyTransition, transitionNode, schemas, scripts)

    newNodes.push(
      { item: transitionNode, type: 'add' },
      {
        source: behaviorPolicyNode.id,
        target: transitionNode.id,
        sourceHandle: null,
        targetHandle: null,
      },
      ...pipelines
    )
  }

  return newNodes
}
