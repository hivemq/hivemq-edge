import type { SelectEntityType } from '@/components/MQTT/types.ts'
import type { EdgeTypes, NodeTypes } from '@/modules/Workspace/types.ts'

export const KEY_FILTER_CONFIGURATIONS = 'edge.workspace.filters'
export const KEY_FILTER_CURRENT = 'edge.workspace.filter'

export interface FilterConfigurationOption {
  label: string
  config: string
}

export interface FilterSelectionOption {
  id: string
  type: NodeTypes | EdgeTypes
}

export interface FilterEntitiesOption {
  label: string
  value: NodeTypes
}

export interface FilterTopicsOption {
  label: string
  value: string
  type: SelectEntityType
}

export interface FilterStatusOption {
  label: string
  status: string
}
