import type { SystemStyleObject } from '@chakra-ui/react'
import type { Node } from '@xyflow/react'
import type { MultiValue } from 'chakra-react-select'
import type {
  ActiveFilter,
  Filter,
  FilterAdapterOption,
  FilterConfigurationOption,
  FilterEntitiesOption,
  FilterSelectionOption,
  FilterStatusOption,
} from '@/modules/Workspace/components/filters/types.ts'
import { type NodeAdapterType, NodeTypes } from '@/modules/Workspace/types.ts'

export const filterContainerStyle = (style: SystemStyleObject) => ({
  ...style,
  minWidth: 'var(--chakra-sizes-3xs)',
  // maxWidth: 'var(--chakra-sizes-xs)',
})

export const applySelectionFilter = (node: Node, criteria?: ActiveFilter<MultiValue<FilterSelectionOption>>) => {
  if (!criteria) return undefined
  if (!criteria.isActive) return undefined
  if (!criteria.filter) return undefined
  if (criteria.filter.length === 0) return undefined

  return criteria.filter.some((e) => e.id === node.id)
}

export const applyEntityFilter = (node: Node, criteria?: ActiveFilter<MultiValue<FilterEntitiesOption>>) => {
  if (!criteria) return undefined
  if (!criteria.isActive) return undefined
  if (!criteria.filter) return undefined
  if (criteria.filter.length === 0) return undefined

  return criteria.filter.some((e) => e.value === node.type)
}

export const applyProtocolFilter = (node: Node, criteria?: ActiveFilter<MultiValue<FilterAdapterOption>>) => {
  if (!criteria) return undefined
  if (!criteria.isActive) return undefined
  if (!criteria.filter) return undefined
  if (criteria.filter.length === 0) return undefined

  return criteria.filter.some(
    (e) => NodeTypes.ADAPTER_NODE === node.type && (node as NodeAdapterType).data.type === e.type
  )
}

export const applyStatusFilter = (node: Node, criteria?: ActiveFilter<MultiValue<FilterStatusOption>>) => {
  if (!criteria) return undefined
  if (!criteria.isActive) return undefined
  if (!criteria.filter) return undefined
  if (criteria.filter.length === 0) return undefined

  return criteria.filter.some(
    (e) =>
      (NodeTypes.ADAPTER_NODE === node.type &&
        ((node as NodeAdapterType).data.status?.connection === e.status ||
          (node as NodeAdapterType).data.status?.runtime === e.status)) ||
      (NodeTypes.BRIDGE_NODE === node.type &&
        ((node as NodeAdapterType).data.status?.connection === e.status ||
          (node as NodeAdapterType).data.status?.runtime === e.status))
  )
}

export const applyQuickFilters = (node: Node, quickFilters: FilterConfigurationOption[]) => {
  const coverage = quickFilters.reduce<boolean[]>((acc, quickFilter) => {
    if (!quickFilter.isActive) {
      return acc
    }
    const isHidden = hideNodeWithFilters(node, quickFilter.filter)
    acc.push(!isHidden)
    return acc
  }, [])

  if (coverage.length === 0) return undefined
  return coverage.includes(true)
}

// TODO[NVL] AND is not yet supported
export const hideNodeWithFilters = (node: Node, state: Filter, quickFilters?: FilterConfigurationOption[]) => {
  // Edge is always shown
  if (node.type === NodeTypes.EDGE_NODE) return false
  const isInQuickFilters = quickFilters ? applyQuickFilters(node, quickFilters) : undefined
  const isInProtocol = applyProtocolFilter(node, state.protocols)
  const isInEntities = applyEntityFilter(node, state.entities)
  const isInSelection = applySelectionFilter(node, state.selection)
  const isInStatus = applyStatusFilter(node, state.status)
  const allTest = []
  if (isInQuickFilters !== undefined) allTest.push(isInQuickFilters)
  if (isInProtocol !== undefined) allTest.push(isInProtocol)
  if (isInEntities !== undefined) allTest.push(isInEntities)
  if (isInSelection !== undefined) allTest.push(isInSelection)
  if (isInStatus !== undefined) allTest.push(isInStatus)

  if (allTest.length === 0) return false

  return !allTest.includes(true)
}
