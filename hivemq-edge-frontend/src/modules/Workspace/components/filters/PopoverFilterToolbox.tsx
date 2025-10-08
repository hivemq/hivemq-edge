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
import { ChevronDownIcon } from '@chakra-ui/icons'
import {
  Button,
  ButtonGroup,
  Popover,
  PopoverArrow,
  PopoverBody,
  PopoverCloseButton,
  PopoverContent,
  PopoverFooter,
  PopoverHeader,
  PopoverTrigger,
  VStack,
} from '@chakra-ui/react'

const PopoverFilterToolbox: FC = () => {
  const { t } = useTranslation()
  const [topics, setTopics] = useState<MultiValue<FilterTopicsOption>>()
  const [status, setStatus] = useState<MultiValue<FilterStatusOption>>()
  const [entities, setEntities] = useState<MultiValue<FilterEntitiesOption>>()
  const [selection, setSelection] = useState<MultiValue<FilterSelectionOption>>([])
  const [isDynamic, setIsDynamic] = useState<boolean>(false)
  const [join, setJoin] = useState<string>('OR')

  return (
    <ButtonGroup variant="outline" isAttached size="sm" m={1}>
      <Popover closeOnBlur={false} isLazy>
        <PopoverTrigger>
          <Button size="sm" variant="ghost" rightIcon={<ChevronDownIcon boxSize="24px" />}>
            {t('workspace.searchToolbox.trigger.aria-label')}
          </Button>
        </PopoverTrigger>
        <PopoverContent minWidth="450px" boxShadow="var(--chakra-shadows-dark-lg)">
          <PopoverArrow />
          <PopoverCloseButton />
          <PopoverHeader> {t('workspace.searchToolbox.title')}</PopoverHeader>
          <PopoverBody overflowY="scroll" maxH="33vh" as={VStack} gap={2}>
            <FilterSelection onChange={setSelection} selection={selection} />
            <FilterEntities onChange={setEntities} />
            <FilterTopics onChange={setTopics} />
            <FilterStatus onChange={setStatus} />
            <OptionsFilter onChangeDynamic={setIsDynamic} onChangeJoin={setJoin} />
          </PopoverBody>
          <PopoverFooter>
            <ApplyFilter
              topics={topics}
              status={status}
              isDynamic={isDynamic}
              entities={entities}
              selection={selection}
              join={join}
            />
            <ConfigurationSelector />
          </PopoverFooter>
        </PopoverContent>
      </Popover>
    </ButtonGroup>
  )
}

export default PopoverFilterToolbox
