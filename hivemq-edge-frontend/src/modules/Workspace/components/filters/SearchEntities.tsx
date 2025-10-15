import type { FC } from 'react'
import { useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import type { NodeSelectionChange } from '@xyflow/react'
import { useReactFlow } from '@xyflow/react'
import {
  ButtonGroup,
  FormControl,
  FormLabel,
  HStack,
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
import { CONFIG_FITVIEW_OPTION } from '@/modules/Workspace/utils/react-flow.utils.ts'

interface SearchEntitiesProps {
  onChange?: (values: string[]) => void
  onNavigate?: (current: string) => void
}

const SearchEntities: FC<SearchEntitiesProps> = ({ onChange, onNavigate }) => {
  const { t } = useTranslation()
  const [search, setSearch] = useState('')
  const [current, setCurrent] = useState<number | null>(null)
  const { fitView } = useReactFlow()
  const { nodes, onNodesChange } = useWorkspaceStore()

  const selectedNodes = useMemo(() => {
    return nodes.filter((e) => e.selected === true).map((e) => e.id)
  }, [nodes])

  const handleNavigate = (direction: 'next' | 'prev') => {
    if (current === null) return
    let newIndex = direction === 'next' ? current + 1 : current - 1
    if (newIndex < 0) newIndex = selectedNodes.length - 1
    if (newIndex >= selectedNodes.length) newIndex = 0
    setCurrent(newIndex)
    fitView({ nodes: [{ id: selectedNodes[newIndex] }], ...CONFIG_FITVIEW_OPTION })
    onNavigate?.(selectedNodes[newIndex])
  }

  const handleClear = (clearValue = false) => {
    onNodesChange(
      nodes.map((node) => {
        const select: NodeSelectionChange = {
          id: node.id,
          type: 'select',
          selected: false,
        }
        return select
      })
    )
    fitView({ ...CONFIG_FITVIEW_OPTION })
    setCurrent(null)
    if (clearValue) setSearch('')
    onChange?.([])
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

    if (foundNodes.length === 0) {
      handleClear()
      return
    }

    // change the selection status of selected node
    onNodesChange(
      nodes.map((node) => {
        const select: NodeSelectionChange = {
          id: node.id,
          type: 'select',
          selected: ids.includes(node.id),
        }
        return select
      })
    )

    setCurrent(0)
    fitView({ nodes: foundNodes, ...CONFIG_FITVIEW_OPTION })
    onChange?.(ids)
  }

  const hasSearchStarted = current !== null && search !== ''

  return (
    <FormControl variant="horizontal" data-testid="toolbox-search">
      <FormLabel fontSize="sm" htmlFor="workspace-search" whiteSpace="nowrap" hidden>
        {t('workspace.searchToolbox.search.label')}
      </FormLabel>
      <HStack>
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
        <ButtonGroup size="sm" isAttached isDisabled={!hasSearchStarted}>
          <IconButton
            data-testid="workspace-search-prev"
            icon={<MdArrowBack />}
            aria-label={t('workspace.searchToolbox.search.previous')}
            onClick={() => handleNavigate('prev')}
          />
          <Text
            whiteSpace="nowrap"
            alignContent="center"
            marginX={2}
            data-testid="workspace-search-counter"
            userSelect="none"
          >
            {t('workspace.searchToolbox.search.matches', {
              index: hasSearchStarted ? current + 1 : 0,
              count: hasSearchStarted ? selectedNodes.length : 0,
            })}
          </Text>

          <IconButton
            data-testid="workspace-search-next"
            icon={<MdArrowForward />}
            aria-label={t('workspace.searchToolbox.search.next')}
            onClick={() => handleNavigate('next')}
          />
        </ButtonGroup>
      </HStack>
    </FormControl>
  )
}

export default SearchEntities
