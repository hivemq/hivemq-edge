import type { FC } from 'react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useReactFlow, useStore } from '@xyflow/react'
import {
  ButtonGroup,
  FormControl,
  FormLabel,
  Icon,
  Input,
  InputGroup,
  InputLeftElement,
  InputRightElement,
  Text,
} from '@chakra-ui/react'
import { SearchIcon } from '@chakra-ui/icons'
import { MdArrowBack, MdArrowForward, MdClear } from 'react-icons/md'

import IconButton from '@/components/Chakra/IconButton.tsx'
import useWorkspaceStore from '@/modules/Workspace/hooks/useWorkspaceStore.ts'
import {
  addSelectedNodesState,
  CONFIG_FITVIEW_OPTION,
  resetSelectedNodesState,
} from '@/modules/Workspace/utils/react-flow.utils.ts'

interface SearchEntitiesProps {
  id?: string
}

const SearchEntities: FC<SearchEntitiesProps> = () => {
  const { t } = useTranslation()
  const [search, setSearch] = useState('')
  const [selectedNodes, setSelectedNodes] = useState<string[]>([])
  const [current, setCurrent] = useState<number | null>(null)
  const { fitView } = useReactFlow()
  const { nodes } = useWorkspaceStore()
  const resetSelectedNodes = useStore(resetSelectedNodesState)
  const addSelectedNodes = useStore(addSelectedNodesState)

  const handleNavigate = (direction: 'next' | 'prev') => {
    if (current === null) return
    let newIndex = direction === 'next' ? current + 1 : current - 1
    if (newIndex < 0) newIndex = selectedNodes.length - 1
    if (newIndex >= selectedNodes.length) newIndex = 0
    setCurrent(newIndex)
    fitView({ nodes: [{ id: selectedNodes[newIndex] }], ...CONFIG_FITVIEW_OPTION })
  }

  const handleClear = (clearValue = false) => {
    resetSelectedNodes()
    fitView({ ...CONFIG_FITVIEW_OPTION })
    setSelectedNodes([])
    setCurrent(null)
    if (clearValue) setSearch('')
  }

  const handleChange = (value: string) => {
    if (!value) {
      handleClear(true)
      return
    }
    const foundNodes = nodes.filter((node) => {
      return new RegExp(value, 'i').test(node.id) && node.hidden !== true
    })
    const ids = foundNodes.map((node) => node.id)
    setSearch(value)
    setSelectedNodes(ids)

    if (foundNodes.length === 0) {
      handleClear()
      return
    }

    addSelectedNodes(ids)
    setCurrent(0)
    fitView({ nodes: foundNodes, ...CONFIG_FITVIEW_OPTION })
  }

  return (
    <FormControl variant="horizontal">
      <FormLabel fontSize="sm" htmlFor="workspace-search" whiteSpace="nowrap" hidden>
        {t('workspace.searchToolbox.search.label')}
      </FormLabel>
      <InputGroup size="sm">
        <InputLeftElement>
          <Icon as={SearchIcon} boxSize="3" />
        </InputLeftElement>
        <Input
          data-testid="workspace-search"
          placeholder={t('workspace.searchToolbox.search.placeholder')}
          size="sm"
          id="workspace-search"
          value={search}
          onChange={(e) => handleChange(e.target.value)}
        />
        {search && (
          <InputRightElement>
            <IconButton
              size="sm"
              variant="ghost"
              data-testid="workspace-search-clear"
              aria-label={t('workspace.searchToolbox.search.clear')}
              icon={<MdClear />}
              onClick={() => {
                handleClear(true)
              }}
            />
          </InputRightElement>
        )}
      </InputGroup>
      <ButtonGroup size="sm" isAttached isDisabled={current === null}>
        <IconButton
          data-testid="workspace-search-prev"
          icon={<MdArrowBack />}
          aria-label={t('workspace.searchToolbox.search.previous')}
          onClick={() => handleNavigate('prev')}
        />
        <Text alignContent="center" marginX={2} data-testid="workspace-search-counter" userSelect="none">
          {current !== null ? current + 1 : 0} of {selectedNodes.length}
        </Text>

        <IconButton
          data-testid="workspace-search-next"
          icon={<MdArrowForward />}
          aria-label={t('workspace.searchToolbox.search.next')}
          onClick={() => handleNavigate('next')}
        />
      </ButtonGroup>
    </FormControl>
  )
}

export default SearchEntities
