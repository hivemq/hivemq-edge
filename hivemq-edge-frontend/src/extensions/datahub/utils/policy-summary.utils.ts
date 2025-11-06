import { v4 as uuidv4 } from 'uuid'
import type { DryRunResults, ResourceSummary, PolicySummary, PolicyPayload } from '@datahub/types.ts'
import { DataHubNodeType, DesignerStatus, ResourceWorkingVersion } from '@datahub/types.ts'
import type { BehaviorPolicy, DataPolicy, PolicySchema, Script } from '@/api/__generated__'

/**
 * Extracts policy summary information from the validation report's final summary item.
 *
 * The final item in the report array contains the complete policy validation with full JSON payload.
 * This function extracts the essential policy information for display in the success summary.
 *
 * @param report - The validation report array from usePolicyChecksStore
 * @param designerStatus - The current designer status (DRAFT = new, MODIFIED = update)
 * @returns PolicySummary object or undefined if report is invalid
 */
export function extractPolicySummary(
  report: DryRunResults<unknown, never>[] | undefined,
  designerStatus: DesignerStatus
): PolicySummary | undefined {
  if (!report || report.length === 0) return undefined

  // Get the final summary item (last in array)
  const finalSummary = [...report].pop()
  if (!finalSummary?.data || !finalSummary.node) return undefined

  const policyType = finalSummary.node.type
  if (policyType !== DataHubNodeType.DATA_POLICY && policyType !== DataHubNodeType.BEHAVIOR_POLICY) {
    return undefined
  }

  const policyData = finalSummary.data as DataPolicy | BehaviorPolicy

  // Determine if this is a new policy or an update
  const isNew = designerStatus === DesignerStatus.DRAFT

  // Extract type-specific details
  const summary: PolicySummary = {
    id: policyData.id,
    type: policyType,
    isNew,
  }

  // For Data Policies, extract topic filter
  if (policyType === DataHubNodeType.DATA_POLICY) {
    const dataPolicy = policyData as DataPolicy
    // Note: API uses singular 'topicFilter' not 'topicFilters'
    summary.topicFilters = dataPolicy.matching?.topicFilter ? [dataPolicy.matching.topicFilter] : []
  }

  // For Behavior Policies, extract transition events
  if (policyType === DataHubNodeType.BEHAVIOR_POLICY) {
    const behaviorPolicy = policyData as BehaviorPolicy
    // Extract event names from each transition's property keys (excluding fromState and toState)
    summary.transitions = behaviorPolicy.onTransitions
      ? behaviorPolicy.onTransitions.flatMap((transition) =>
          Object.keys(transition).filter((key) => key !== 'fromState' && key !== 'toState')
        )
      : []
  }

  return summary
}

/**
 * Extracts resource summary information from the validation report's final summary item.
 *
 * The final item contains all resources (schemas and scripts) in its resources array.
 * This function extracts essential metadata for each resource for display.
 *
 * @param report - The validation report array from usePolicyChecksStore
 * @returns Array of ResourceSummary objects
 */
export function extractResourcesSummary(report: DryRunResults<unknown, never>[] | undefined): ResourceSummary[] {
  if (!report || report.length === 0) return []

  // Get the final summary item (last in array)
  const finalSummary = [...report].pop()
  if (!finalSummary?.resources) return []

  // Extract summary for each resource
  return finalSummary.resources
    .filter((resource) => {
      const version = (resource.node.data as { version?: ResourceWorkingVersion | number })?.version

      // Only process schemas and scripts
      return (
        (resource.node.type === DataHubNodeType.SCHEMA || resource.node.type === DataHubNodeType.FUNCTION) &&
        (version === ResourceWorkingVersion.DRAFT || version === ResourceWorkingVersion.MODIFIED)
      )
    })
    .map((resource): ResourceSummary => {
      const nodeType = resource.node.type
      const version = (resource.node.data as { version?: ResourceWorkingVersion | number })?.version

      // Determine if this is a new resource or an update
      const isNew = version === ResourceWorkingVersion.DRAFT

      const summary: ResourceSummary = {
        id: uuidv4(),
        version: version || ResourceWorkingVersion.DRAFT,
        type: nodeType === DataHubNodeType.SCHEMA ? 'SCHEMA' : 'FUNCTION',
        isNew,
        metadata: {},
      }

      // Extract type-specific metadata
      if (nodeType === DataHubNodeType.SCHEMA && resource.data) {
        const schema = resource.data as PolicySchema
        summary.id = schema.id
        summary.metadata.schemaType = schema.type as 'JSON' | 'PROTOBUF'
      }

      if (nodeType === DataHubNodeType.FUNCTION && resource.data) {
        const script = resource.data as Script
        summary.id = script.id
        summary.metadata.functionType = script.functionType as string
      }

      return summary
    })
}

/**
 * Extracts the complete policy payload for JSON display from the validation report.
 *
 * Returns the complete policy JSON and all associated resources (schemas and scripts).
 * This data is used for the optional JSON payload view.
 *
 * @param report - The validation report array from usePolicyChecksStore
 * @returns PolicyPayload object or undefined if report is invalid
 */
export function extractPolicyPayload(report: DryRunResults<unknown, never>[] | undefined): PolicyPayload | undefined {
  if (!report || report.length === 0) return undefined

  // Get the final summary item (last in array)
  const finalSummary = [...report].pop()
  if (!finalSummary?.data) return undefined

  const payload: PolicyPayload = {
    policy: finalSummary.data as object,
    resources: {
      schemas: [],
      scripts: [],
    },
  }

  // Extract resources if present
  if (finalSummary.resources) {
    finalSummary.resources.forEach((resource) => {
      if (resource.node.type === DataHubNodeType.SCHEMA && resource.data) {
        payload.resources.schemas.push(resource.data as object)
      }
      if (resource.node.type === DataHubNodeType.FUNCTION && resource.data) {
        payload.resources.scripts.push(resource.data as object)
      }
    })
  }

  return payload
}

/**
 * Groups resources by type (schemas and scripts) for organized display.
 *
 * @param resources - Array of ResourceSummary objects
 * @returns Object with schemas and scripts arrays
 */
export function groupResourcesByType(resources: ResourceSummary[]): {
  schemas: ResourceSummary[]
  scripts: ResourceSummary[]
} {
  return {
    schemas: resources.filter((r) => r.type === 'SCHEMA'),
    scripts: resources.filter((r) => r.type === 'FUNCTION'),
  }
}
