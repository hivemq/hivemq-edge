import { Node, NodeAddChange, XYPosition } from 'reactflow'

import {
  BehaviorPolicyData,
  BehaviorPolicyType,
  DataHubNodeType,
  DryRunResults,
  WorkspaceAction,
  WorkspaceState,
} from '@datahub/types.ts'
import { PolicyCheckErrors } from '@datahub/designer/validation.errors.ts'
import { BehaviorPolicy, BehaviorPolicyBehavior, BehaviorPolicyOnTransition } from '@/api/__generated__'
import { enumFromStringValue } from '@/utils/types.utils.ts'

export function checkValidityModel(behaviorPolicy: Node<BehaviorPolicyData>): DryRunResults<BehaviorPolicyBehavior> {
  if (!behaviorPolicy.data.model) {
    return {
      node: behaviorPolicy,
      error: PolicyCheckErrors.notConfigured(behaviorPolicy, 'model'),
    }
  }
  return {
    node: behaviorPolicy,
    data: {
      arguments: behaviorPolicy.data.arguments,
      id: behaviorPolicy.data.model,
    },
  }
}

// TODO[NVL] Need to find a better way of testing this one
/* istanbul ignore next -- @preserve */
export function checkValidityBehaviorPolicy(
  node: Node<BehaviorPolicyData>,
  client: DryRunResults<string, never>,
  model: DryRunResults<BehaviorPolicyBehavior, never>,
  transitions: DryRunResults<BehaviorPolicyOnTransition, never>[]
): DryRunResults<BehaviorPolicy, unknown> {
  return {
    node: node,
    data: {
      behavior: model.data as BehaviorPolicyBehavior,
      // TODO[19466] Id should be user-facing; Need to fix before merging!
      id: node.id,
      matching: { clientIdRegex: client.data as string },
      onTransitions: transitions.length
        ? transitions.map((transition) => {
            return transition.data as BehaviorPolicyOnTransition
          })
        : undefined,
    },
  }
}

export const loadBehaviorPolicy = (behaviorPolicy: BehaviorPolicy, store: WorkspaceState & WorkspaceAction) => {
  const { onNodesChange } = store

  const model = enumFromStringValue(BehaviorPolicyType, behaviorPolicy.behavior.id)
  if (!model) throw new Error('something wrong with the behavior policy')

  const position: XYPosition = {
    x: 0,
    y: 0,
  }

  const behaviorPolicyNode: Node<BehaviorPolicyData> = {
    id: behaviorPolicy.id,
    type: DataHubNodeType.BEHAVIOR_POLICY,
    position,
    data: {
      model: model,
      arguments: behaviorPolicy.behavior.arguments,
    },
  }

  onNodesChange([{ item: behaviorPolicyNode, type: 'add' } as NodeAddChange])
}
