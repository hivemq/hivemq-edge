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
