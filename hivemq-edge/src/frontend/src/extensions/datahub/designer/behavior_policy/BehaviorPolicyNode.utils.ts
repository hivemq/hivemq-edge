import { Node, NodeAddChange, XYPosition } from 'reactflow'

import { BehaviorPolicy, BehaviorPolicyBehavior, BehaviorPolicyOnTransition } from '@/api/__generated__'
import { enumFromStringValue } from '@/utils/types.utils.ts'
import i18n from '@/config/i18n.config.ts'

import { PolicyCheckErrors } from '@datahub/designer/validation.errors.ts'
import { BehaviorPolicyData, BehaviorPolicyType, DataHubNodeType, DryRunResults } from '@datahub/types.ts'

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

export const loadBehaviorPolicy = (behaviorPolicy: BehaviorPolicy): NodeAddChange => {
  const model = enumFromStringValue(BehaviorPolicyType, behaviorPolicy.behavior.id)
  if (!model)
    throw new Error(
      i18n.t('datahub:error.loading.connection.notFound', { type: DataHubNodeType.BEHAVIOR_POLICY }) as string
    )

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

  return { item: behaviorPolicyNode, type: 'add' }
}
