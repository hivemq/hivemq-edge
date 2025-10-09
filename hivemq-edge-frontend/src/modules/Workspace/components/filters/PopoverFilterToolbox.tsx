import type { FC } from 'react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import type { MultiValue } from 'chakra-react-select'
import {
  ApplyFilter,
  ConfigurationSelector,
  OptionsFilter,
  FilterEntities,
  FilterSelection,
  FilterStatus,
  FilterTopics,
} from '@/modules/Workspace/components/filters/index.ts'
import type {
  FilterEntitiesOption,
  FilterSelectionOption,
  FilterStatusOption,
  FilterTopicsOption,
} from '@/modules/Workspace/components/filters/types.ts'
import {
  Button,
  ButtonGroup,
  Drawer,
  DrawerBody,
  DrawerFooter,
  DrawerHeader,
  DrawerContent,
  DrawerCloseButton,
  VStack,
  useDisclosure,
  Icon,
} from '@chakra-ui/react'
import { GoSidebarExpand } from 'react-icons/go'

const PopoverFilterToolbox: FC = () => {
  const { t } = useTranslation()
  const [topics, setTopics] = useState<MultiValue<FilterTopicsOption>>()
  const [status, setStatus] = useState<MultiValue<FilterStatusOption>>()
  const [entities, setEntities] = useState<MultiValue<FilterEntitiesOption>>()
  const [selection, setSelection] = useState<MultiValue<FilterSelectionOption>>([])
  const [isDynamic, setIsDynamic] = useState<boolean>(false)
  const [join, setJoin] = useState<string>('OR')
  const { isOpen, onOpen, onClose } = useDisclosure()

  return (
    <ButtonGroup variant="outline" isAttached size="sm" m={1}>
      <Button
        size="sm"
        variant="ghost"
        leftIcon={<Icon as={GoSidebarExpand} boxSize="24px" />}
        // rightIcon={<Icon as={TbFilter} boxSize="24px" />}
        onClick={onOpen}
      >
        {t('workspace.searchToolbox.trigger.aria-label')}
      </Button>
      <Drawer isOpen={isOpen} placement="right" size="sm" onClose={onClose}>
        {/*<DrawerOverlay />*/}
        <DrawerContent>
          <DrawerCloseButton />
          <DrawerHeader> {t('workspace.searchToolbox.title')}</DrawerHeader>

          <DrawerBody as={VStack} gap={4}>
            <FilterSelection onChange={setSelection} selection={selection} />
            <FilterEntities onChange={setEntities} />
            <FilterTopics onChange={setTopics} />
            <FilterStatus onChange={setStatus} />
            <OptionsFilter onChangeDynamic={setIsDynamic} onChangeJoin={setJoin} />
            <ConfigurationSelector />
          </DrawerBody>

          <DrawerFooter>
            <ApplyFilter
              topics={topics}
              status={status}
              isDynamic={isDynamic}
              entities={entities}
              selection={selection}
              join={join}
            />
          </DrawerFooter>
        </DrawerContent>
      </Drawer>
    </ButtonGroup>
  )
}

export default PopoverFilterToolbox
