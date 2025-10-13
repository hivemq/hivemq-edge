import type { FC } from 'react'
import type { MultiValue, PropsValue } from 'chakra-react-select'
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

export interface FilterAdapterOption {
  label: string
  type: string
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

export interface FilterOperationOption {
  isLiveUpdate: string
  joinOperator: 'OR' | 'AND'
}

export interface ActiveFilter<T extends PropsValue<any>> {
  isActive: boolean
  filter?: T
}

export interface Filter {
  entities?: ActiveFilter<MultiValue<FilterEntitiesOption>>
  protocols?: ActiveFilter<MultiValue<FilterAdapterOption>>
  selection?: ActiveFilter<MultiValue<FilterSelectionOption>>
  topic?: ActiveFilter<MultiValue<FilterTopicsOption>>
  status?: ActiveFilter<MultiValue<FilterStatusOption>>
}

export interface FilerConfig extends Filter {
  quickFilters?: Array<Filter>
  options?: FilterOperationOption
}

export interface FilterEditorProps {
  id: keyof Filter
  label: string
  // TODO Fix the type for the filter component
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  editor: FC<any>
}

export interface FilterCriteriaEditorProps<T> {
  onChange?: (values: PropsValue<T>) => void
  selection?: PropsValue<FilterSelectionOption>
}
