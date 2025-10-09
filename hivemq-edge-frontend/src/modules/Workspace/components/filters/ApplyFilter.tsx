import type { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, FormControl, FormLabel } from '@chakra-ui/react'
import type { NodeReplaceChange } from '@xyflow/react'
import { useReactFlow } from '@xyflow/react'
import type { MultiValue } from 'chakra-react-select'

import type {
  FilterEntitiesOption,
  FilterSelectionOption,
  FilterStatusOption,
  FilterTopicsOption,
} from '@/modules/Workspace/components/filters/types.ts'
import useWorkspaceStore from '@/modules/Workspace/hooks/useWorkspaceStore.ts'
import { NodeTypes } from '@/modules/Workspace/types.ts'

interface ApplyFilterProps {
  id?: string
  topics?: MultiValue<FilterTopicsOption>
  status?: MultiValue<FilterStatusOption>
  entities?: MultiValue<FilterEntitiesOption>
  selection?: MultiValue<FilterSelectionOption>
  isDynamic?: boolean
  join?: string
}

const ApplyFilter: FC<ApplyFilterProps> = ({ topics, status, entities, selection, isDynamic, join }) => {
  const { t } = useTranslation()
  const { nodes, onNodesChange } = useWorkspaceStore()
  const { fitView } = useReactFlow()

  const handleClearFilters = () => {
    const changeContent = nodes.map<NodeReplaceChange>((node) => ({
      id: node.id,
      item: { ...node, hidden: false },
      type: 'replace',
    }))
    onNodesChange(changeContent)
    fitView({ padding: 0.25, duration: 750 })
  }

  const handleFilter = () => {
    // if (!selection?.length) {
    //   // handleClearFilters()
    //   return
    // }

    const changeContent = nodes.map<NodeReplaceChange>((node) => {
      const isSelection = selection?.some((e) => e.id === node.id)
      const isWithinEntities = entities?.some((e) => e.value === node.type)
      console.log('XXXX', isWithinEntities)
      const isEdge = node.type === NodeTypes.EDGE_NODE
      return {
        id: node.id,
        item: { ...node, hidden: !isSelection && !isEdge && !isWithinEntities },
        type: 'replace',
      }
    })
    onNodesChange(changeContent)
    fitView({ padding: 0.25, duration: 750 })
  }

  const handleApply = () => {
    console.log('XXXXXXX', { topics, status, entities, selection, isDynamic, join })
    handleFilter()
  }

  return (
    <FormControl variant="horizontal">
      <FormLabel fontSize="sm" htmlFor="workspace-filter-apply"></FormLabel>
      <Button onClick={() => handleApply()}>{t('Apply filters')} </Button>
      <Button onClick={() => handleClearFilters()}>{t('Clear filters')} </Button>
    </FormControl>
  )
}

export default ApplyFilter
