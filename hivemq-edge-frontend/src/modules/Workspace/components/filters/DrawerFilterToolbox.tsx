import type { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { useLocalStorage } from '@uidotdev/usehooks'
import type { NodeReplaceChange } from '@xyflow/react'
import { useReactFlow } from '@xyflow/react'
import type { MultiValue } from 'chakra-react-select'
import {
  Button,
  ButtonGroup,
  Drawer,
  DrawerBody,
  DrawerCloseButton,
  DrawerContent,
  DrawerFooter,
  DrawerHeader,
  Icon,
  useDisclosure,
  VStack,
} from '@chakra-ui/react'
import { GoSidebarExpand } from 'react-icons/go'

import {
  FilterEntities,
  FilterSelection,
  FilterProtocol,
  FilterStatus,
  FilterTopics,
  OptionsFilter,
  WrapperCriteria,
  QuickFilters,
  ApplyFilter,
} from '@/modules/Workspace/components/filters/index.ts'
import { hideNodeWithFilters } from '@/modules/Workspace/components/filters/filters.utils.ts'

import type {
  ActiveFilter,
  FilterConfig,
  Filter,
  FilterEditorProps,
  FilterEntitiesOption,
  FilterSelectionOption,
  FilterStatusOption,
  FilterTopicsOption,
} from '@/modules/Workspace/components/filters/types.ts'
import { KEY_FILTER_CURRENT } from '@/modules/Workspace/components/filters/types.ts'
import useWorkspaceStore from '@/modules/Workspace/hooks/useWorkspaceStore.ts'

interface DrawerFilterToolboxProps {
  onClearFilters?: () => void
  onApplyFilters?: (currentState: FilterConfig) => void
}

const DrawerFilterToolbox: FC<DrawerFilterToolboxProps> = ({ onClearFilters, onApplyFilters }) => {
  const { t } = useTranslation()
  const { isOpen, onOpen, onClose } = useDisclosure()
  const [currentState, setCurrentState] = useLocalStorage<FilterConfig>(KEY_FILTER_CURRENT, {
    options: { isLiveUpdate: false, joinOperator: 'OR' },
  })
  const { nodes, onNodesChange } = useWorkspaceStore()
  const { fitView } = useReactFlow()

  const filterEditors: FilterEditorProps[] = [
    { id: 'selection', label: t('workspace.searchToolbox.bySelection.criteria'), editor: FilterSelection },
    { id: 'entities', label: t('workspace.searchToolbox.byEntity.criteria'), editor: FilterEntities },
    { id: 'protocols', label: t('workspace.searchToolbox.byProtocol.criteria'), editor: FilterProtocol },
    { id: 'topic', label: t('workspace.searchToolbox.byTopics.criteria'), editor: FilterTopics },
    { id: 'status', label: t('workspace.searchToolbox.byStatus.criteria'), editor: FilterStatus },
  ]

  const isAnyFilterActive = Object.values(currentState).some((e: ActiveFilter<never>) => e.isActive)

  const handleClearFilters = () => {
    const changeContent = nodes.map<NodeReplaceChange>((node) => ({
      id: node.id,
      item: { ...node, hidden: false },
      type: 'replace',
    }))
    onNodesChange(changeContent)
    fitView({ padding: 0.25, duration: 750 })
    setCurrentState({})
    onClearFilters?.()
  }

  const handleFilter = (state: Filter) => {
    const changeContent = nodes.map<NodeReplaceChange>((node) => {
      return {
        id: node.id,
        item: {
          ...node,
          hidden: hideNodeWithFilters(node, state),
        },
        type: 'replace',
      }
    })
    onNodesChange(changeContent)
    fitView({ padding: 0.25, duration: 750 })
    onApplyFilters?.(state)
  }

  const handleChange = (id: keyof Filter) => {
    return (
      value:
        | MultiValue<FilterSelectionOption>
        | MultiValue<FilterEntitiesOption>
        | MultiValue<FilterTopicsOption>
        | MultiValue<FilterStatusOption>
    ) => {
      const newState = { ...currentState, [id]: { isActive: true, filter: value } }
      setCurrentState(newState)
      handleFilter(newState)
    }
  }

  const handleActive = (id: string) => (value: boolean) => {
    const current = currentState[id as keyof Filter]
    const newState = { ...currentState, [id]: { isActive: value, filter: current?.filter } }
    setCurrentState(newState)
    handleFilter(newState)
  }

  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const handleNewQuickFiler = (_name: string) => {
    handleClearFilters()
  }

  return (
    <ButtonGroup variant="outline" isAttached size="sm" m={1} data-testid="toolbox-filter">
      <Button
        size="sm"
        leftIcon={<Icon as={GoSidebarExpand} boxSize="24px" />}
        // rightIcon={<Icon as={TbFilter} boxSize="24px" />}
        onClick={onOpen}
        data-testid="toolbox-filter-open"
      >
        {t('workspace.searchToolbox.trigger.aria-label')}
      </Button>
      <Button onClick={handleClearFilters} isDisabled={!isAnyFilterActive} data-testid="toolbox-filter-clearAll">
        {t('workspace.searchToolbox.action.clear')}
      </Button>

      <Drawer isOpen={isOpen} placement="right" size="sm" onClose={onClose} id="filter-workspace">
        <DrawerContent>
          <DrawerCloseButton />
          <DrawerHeader>{t('workspace.searchToolbox.title')}</DrawerHeader>

          <DrawerBody as={VStack} gap={4}>
            <QuickFilters isFilterActive={isAnyFilterActive} onNewQuickFilter={handleNewQuickFiler} />
            {filterEditors.map((criteria) => (
              <WrapperCriteria
                key={criteria.id}
                label={criteria.label}
                id={criteria.id}
                isActive={currentState[criteria.id]?.isActive || false}
                onChange={handleActive(criteria.id)}
              >
                <criteria.editor onChange={handleChange(criteria.id)} value={currentState[criteria.id]?.filter} />
              </WrapperCriteria>
            ))}
            <OptionsFilter value={currentState.options} />
          </DrawerBody>
          <DrawerFooter mt={4}>
            <ApplyFilter onClear={handleClearFilters} onSubmit={() => handleFilter(currentState)} />
          </DrawerFooter>
        </DrawerContent>
      </Drawer>
    </ButtonGroup>
  )
}

export default DrawerFilterToolbox
