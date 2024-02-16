import { getOutgoers, Node } from 'reactflow'

import { BehaviorPolicyData, DataHubNodeType, DryRunResults, TransitionData, WorkspaceState } from '@datahub/types.ts'
import { PolicyCheckErrors } from '@datahub/designer/validation.errors.ts'
import { BehaviorPolicyOnTransition } from '@/api/__generated__'

export function checkValidityTransitions(
  behaviorPolicyData: Node<BehaviorPolicyData>,
  store: WorkspaceState
): DryRunResults<BehaviorPolicyOnTransition>[] {
  const { nodes, edges } = store

  const transitions = getOutgoers(behaviorPolicyData, nodes, edges).filter(
    (node) => node.type === DataHubNodeType.TRANSITION
  ) as Node<TransitionData>[]

  if (!transitions.length) {
    return [
      {
        node: behaviorPolicyData,
        error: PolicyCheckErrors.notConnected(DataHubNodeType.TRANSITION, behaviorPolicyData),
      },
    ]
  }

  return transitions.map<DryRunResults<BehaviorPolicyOnTransition>>((transition) => {
    if (!transition.data.event || !transition.data.from || !transition.data.to) {
      return {
        node: transition,
        error: PolicyCheckErrors.notConfigured(transition, 'type, version'),
      }
    }
    return {
      node: transition,
      data: {
        fromState: transition.data.from,
        toState: transition.data.to,
        // TODO[19240] This is wrong. Pipeline is missing
      },
    }
  })
}
