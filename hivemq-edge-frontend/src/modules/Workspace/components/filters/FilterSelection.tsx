import type { FC } from 'react'
import { useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { ButtonGroup, FormControl, FormLabel, HStack, Text } from '@chakra-ui/react'
import type { MultiValue } from 'chakra-react-select'
import { BiSelection } from 'react-icons/bi'
import { MdClear } from 'react-icons/md'

import IconButton from '@/components/Chakra/IconButton.tsx'
import type { FilterCriteriaProps, FilterSelectionOption } from '@/modules/Workspace/components/filters/types.ts'
import useWorkspaceStore from '@/modules/Workspace/hooks/useWorkspaceStore.ts'
import type { NodeTypes } from '@/modules/Workspace/types.ts'

type FilterEntitiesProps = FilterCriteriaProps<MultiValue<FilterSelectionOption>>

const FilterSelection: FC<FilterEntitiesProps> = ({ value, onChange }) => {
  const { t } = useTranslation()
  const { nodes } = useWorkspaceStore()

  const selectedNodes = useMemo(() => {
    return nodes.filter((node) => node.selected)
  }, [nodes])

  const handleSelect = () => {
    onChange?.(selectedNodes.map<FilterSelectionOption>((e) => ({ id: e.id, type: e.type as NodeTypes })))
  }

  const handleClearSelect = () => {
    onChange?.([])
  }

  return (
    <FormControl variant="horizontal" id="workspace-filter-selection">
      <FormLabel fontSize="sm" htmlFor="workspace-filter-selection-items">
        {t('workspace.searchToolbox.bySelection.label')}
      </FormLabel>
      <HStack justifyContent="space-between">
        <Text id="workspace-filter-selection-items">
          {Boolean(value?.length) && t('workspace.searchToolbox.bySelection.nodeFiltered', { count: value?.length })}
          {!value?.length && t('workspace.searchToolbox.bySelection.nodeSelected', { count: selectedNodes.length })}
        </Text>
        <ButtonGroup isAttached size="sm">
          <IconButton
            icon={<BiSelection />}
            aria-label={t('workspace.searchToolbox.bySelection.select')}
            isDisabled={!selectedNodes.length}
            onClick={handleSelect}
            data-testid="workspace-filter-selection-add"
          />

          <IconButton
            icon={<MdClear />}
            aria-label={t('workspace.searchToolbox.bySelection.clear')}
            onClick={handleClearSelect}
            isDisabled={!value?.length}
            data-testid="workspace-filter-selection-clear"
          />
        </ButtonGroup>
      </HStack>
    </FormControl>
  )
}

export default FilterSelection
