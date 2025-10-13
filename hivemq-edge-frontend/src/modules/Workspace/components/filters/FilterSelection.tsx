import type { FC } from 'react'
import { useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { ButtonGroup, FormControl, FormLabel, HStack, Text } from '@chakra-ui/react'
import type { MultiValue } from 'chakra-react-select'
import { BiSelection } from 'react-icons/bi'
import { MdClear } from 'react-icons/md'

import IconButton from '@/components/Chakra/IconButton.tsx'
import type { FilterSelectionOption } from '@/modules/Workspace/components/filters/types.ts'
import useWorkspaceStore from '@/modules/Workspace/hooks/useWorkspaceStore.ts'
import { NodeTypes } from '@/modules/Workspace/types.ts'

interface FilterEntitiesProps {
  onChange?: (values: MultiValue<FilterSelectionOption>) => void
  value?: MultiValue<FilterSelectionOption>
}

const FilterSelection: FC<FilterEntitiesProps> = ({ value, onChange }) => {
  const { t } = useTranslation()
  // const [selection, setSelection] = useState<string[]>([])
  const { nodes } = useWorkspaceStore()

  const selectedNodes = useMemo(() => {
    return nodes.filter((node) => node.selected)
  }, [nodes])

  const handleSelect = () => {
    // setSelection(selectedNodes.map((e) => e.id))
    onChange?.(selectedNodes.map<FilterSelectionOption>((e) => ({ id: e.id, type: NodeTypes.ADAPTER_NODE })))
  }

  const handleClearSelect = () => {
    // setSelection([])
    onChange?.([])
  }

  return (
    <FormControl variant="horizontal">
      <FormLabel fontSize="sm" htmlFor="workspace-filter-selection">
        {t('workspace.searchToolbox.bySelection.label')}
      </FormLabel>
      <HStack justifyContent="space-between">
        <Text id="workspace-filter-selection">
          {Boolean(value?.length) && t('workspace.searchToolbox.bySelection.nodeFiltered', { count: value?.length })}
          {!value?.length && t('workspace.searchToolbox.bySelection.nodeSelected', { count: selectedNodes.length })}
        </Text>
        <ButtonGroup isAttached size="sm">
          <IconButton
            icon={<BiSelection />}
            aria-label={t('workspace.searchToolbox.bySelection.select')}
            isDisabled={!selectedNodes.length}
            onClick={handleSelect}
          />

          <IconButton
            icon={<MdClear />}
            aria-label={t('workspace.searchToolbox.bySelection.clear')}
            onClick={handleClearSelect}
            isDisabled={!value?.length}
          />
        </ButtonGroup>
      </HStack>
    </FormControl>
  )
}

export default FilterSelection
