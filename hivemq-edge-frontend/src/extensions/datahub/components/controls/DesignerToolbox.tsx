import type { FC } from 'react'
import { useTranslation } from 'react-i18next'
import {
  Popover,
  PopoverTrigger,
  PopoverContent,
  PopoverHeader,
  PopoverBody,
  PopoverArrow,
  PopoverCloseButton,
  Text,
  VStack,
  IconButton,
  Icon,
  HStack,
} from '@chakra-ui/react'
import { ChevronDownIcon, ChevronRightIcon } from '@chakra-ui/icons'
import { FaTools } from 'react-icons/fa'

import Panel from '@/components/react-flow/Panel.tsx'
import { ToolboxNodes } from '@datahub/components/controls/ToolboxNodes.tsx'
import DraftStatus from '@datahub/components/helpers/DraftStatus.tsx'
import { usePolicyGuards } from '@datahub/hooks/usePolicyGuards.ts'

const DesignerToolbox: FC = () => {
  const { t } = useTranslation('datahub')
  const { isPolicyEditable } = usePolicyGuards()

  return (
    <Panel position="top-left">
      <HStack role="group" aria-label={t('workspace.toolbars.draft.aria-label')} p={1}>
        <Popover>
          {({ isOpen }) => (
            <>
              <PopoverTrigger>
                <IconButton
                  isDisabled={!isPolicyEditable}
                  data-testid="toolbox-trigger"
                  aria-label={t('workspace.toolbox.trigger', { context: !isOpen ? 'open' : 'close' })}
                  aria-controls="toolbox-content"
                  icon={
                    <>
                      <Icon as={FaTools} />
                      <Icon as={isOpen ? ChevronDownIcon : ChevronRightIcon} ml={2} boxSize="24px" />
                    </>
                  }
                  px={2}
                />
              </PopoverTrigger>
              <PopoverContent width="unset" id="toolbox-content" data-testid="toolbox-container">
                <PopoverArrow />
                <PopoverCloseButton />
                <PopoverHeader>{t('workspace.toolbox.panel.aria-label')}</PopoverHeader>
                <PopoverBody as={VStack} alignItems="flex-start" maxWidth="12rem">
                  <Text fontSize="sm">{t('workspace.toolbox.panel.helper')}</Text>
                  <ToolboxNodes direction="vertical" />
                </PopoverBody>
              </PopoverContent>
            </>
          )}
        </Popover>
        <DraftStatus />
      </HStack>
    </Panel>
  )
}

export default DesignerToolbox
