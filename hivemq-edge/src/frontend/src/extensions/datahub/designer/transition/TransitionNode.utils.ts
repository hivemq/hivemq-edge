import { getOutgoers, Node } from 'reactflow'

import { BehaviorPolicyData, DataHubNodeType, DryRunResults, TransitionData, WorkspaceState } from '@datahub/types.ts'
import { PolicyCheckErrors } from '@datahub/designer/validation.errors.ts'
import { BehaviorPolicyOnTransition, PolicyOperation } from '@/api/__generated__'
import { checkValidityPipeline } from '@datahub/designer/operation/OperationNode.utils.ts'

export function checkValidityTransitions(
  behaviorPolicyData: Node<BehaviorPolicyData>,
  store: WorkspaceState
): {
  behaviorPolicyTransitions: DryRunResults<BehaviorPolicyOnTransition>[]
  pipelines?: DryRunResults<PolicyOperation>[]
} {
  const { nodes, edges } = store

  const transitions = getOutgoers(behaviorPolicyData, nodes, edges).filter(
    (node) => node.type === DataHubNodeType.TRANSITION
  ) as Node<TransitionData>[]

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

    return {
      node: transition,
      data: {
        fromState: transition.data.from,
        toState: transition.data.to,
        [transition.data.event]: { pipelines: validPipeline.map((operation) => operation.data) },
      },
    }
  })

  return { behaviorPolicyTransitions, pipelines }
}
