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
} from '@chakra-ui/react'
import type { FC } from 'react'
import { useTranslation } from 'react-i18next'

const FilterToolbox: FC = () => {
  const { t } = useTranslation()

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
          <PopoverBody overflowY="scroll" maxH="33vh"></PopoverBody>
          <PopoverFooter></PopoverFooter>
        </PopoverContent>
      </Popover>
    </ButtonGroup>
  )
}

export default FilterToolbox
