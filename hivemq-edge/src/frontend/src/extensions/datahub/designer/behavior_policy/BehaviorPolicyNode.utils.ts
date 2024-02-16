import { Node } from 'reactflow'

import { BehaviorPolicyData, DryRunResults } from '@datahub/types.ts'
import { PolicyCheckErrors } from '@datahub/designer/validation.errors.ts'
import { BehaviorPolicy, BehaviorPolicyBehavior, BehaviorPolicyOnTransition } from '@/api/__generated__'

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
      // TODO[19240] Id is not handled (like in many nodes); use UUID default?
      id: node.id,
      matching: { clientIdRegex: client.data as string },
      onTransitions: transitions.length
        ? transitions.map((transition) => {
            return {
              fromState: transition.data?.fromState as string,
              toState: transition.data?.toState as string,
            }
          })
        : undefined,
    },
  }
}
