import type { BehaviorPolicyType, FiniteStateMachineSchema } from '@datahub/types.ts'
import { FsmState } from '@datahub/types.ts'
import BEHAVIOR_POLICY_SCHEMA from '@datahub/api/__generated__/schemas/BehaviorPolicyData.json'

export interface ModelMetadata {
  id: BehaviorPolicyType
  title: string
  description: string
  requiresArguments: boolean
  stateCount: number
  transitionCount: number
  hasSuccessState: boolean
  hasFailedState: boolean
}

export const extractModelMetadata = (): ModelMetadata[] => {
  const definitions = BEHAVIOR_POLICY_SCHEMA.definitions as Record<
    string,
    FiniteStateMachineSchema & {
      properties?: { arguments?: { title?: string; description?: string; required?: string[] } }
    }
  >

  return Object.keys(definitions).map((modelId) => {
    const definition = definitions[modelId]
    const { metadata, properties } = definition
    const { states = [], transitions = [] } = metadata || {}
    const argumentsSpec = properties?.arguments

    const hasSuccessState = states.some((state) => state.type === FsmState.Type.SUCCESS)
    const hasFailedState = states.some((state) => state.type === FsmState.Type.FAILED)
    const requiresArguments = (argumentsSpec?.required?.length || 0) > 0

    return {
      id: modelId as BehaviorPolicyType,
      title: argumentsSpec?.title || modelId,
      description: argumentsSpec?.description || '',
      requiresArguments,
      stateCount: states.length,
      transitionCount: transitions.length,
      hasSuccessState,
      hasFailedState,
    }
  })
}
