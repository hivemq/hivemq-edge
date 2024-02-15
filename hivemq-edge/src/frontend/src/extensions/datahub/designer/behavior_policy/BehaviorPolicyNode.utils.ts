import { Node } from 'reactflow'

import { BehaviorPolicyData, DryRunResults } from '@datahub/types.ts'
import { PolicyCheckErrors } from '@datahub/designer/validation.errors.ts'

export function checkValidityModel(behaviorPolicy: Node<BehaviorPolicyData>): DryRunResults<string> {
  if (!behaviorPolicy.data.model) {
    return {
      node: behaviorPolicy,
      error: PolicyCheckErrors.notConfigured(behaviorPolicy, 'model'),
    }
  }
  return {
    node: behaviorPolicy,
  }
}
