import { EntityType, type EntityReference } from '@/api/__generated__'
import { IdStubs } from '@/modules/Workspace/types.ts'
import { NODE_PULSE_AGENT_DEFAULT_ID } from '@/modules/Workspace/utils/nodes-utils.ts'

/* istanbul ignore next -- @preserve */
export const DEFAULT_ASSET_MAPPER_SOURCES: EntityReference[] = [
  { id: IdStubs.EDGE_NODE, type: EntityType.EDGE_BROKER },
  { id: NODE_PULSE_AGENT_DEFAULT_ID, type: EntityType.PULSE_AGENT },
]
