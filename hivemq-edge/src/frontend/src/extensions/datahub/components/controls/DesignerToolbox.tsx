import { FC, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { motion } from 'framer-motion'
import { Box, HStack, Icon, IconButton, useDisclosure } from '@chakra-ui/react'
import { FaTools } from 'react-icons/fa'
import { LuPanelLeftOpen, LuPanelRightOpen } from 'react-icons/lu'

import Panel from '@/components/react-flow/Panel.tsx'
import { ToolboxNodes } from '@datahub/components/controls/ToolboxNodes.tsx'
import DraftStatus from '@datahub/components/helpers/DraftStatus.tsx'

const DesignerToolbox: FC = () => {
  const { t } = useTranslation('datahub')
  const { getButtonProps, getDisclosureProps, isOpen } = useDisclosure()
  const [hidden, setHidden] = useState(!isOpen)

  return (
    <Panel position="top-left">
      <HStack alignItems="center" userSelect="none">
        <Box>
          <IconButton
            data-testid="toolbox-trigger"
            aria-label={t('workspace.toolbox.trigger', { context: !isOpen ? 'open' : 'close' })}
            icon={
              <>
                <Icon as={FaTools} />
                <Icon as={isOpen ? LuPanelRightOpen : LuPanelLeftOpen} ml={2} boxSize="24px" />
              </>
            }
            {...getButtonProps()}
            px={2}
          />
        </Box>
        <motion.div
          {...getDisclosureProps()}
          data-testid="toolbox-container"
          hidden={hidden}
          initial={false}
          onAnimationStart={() => setHidden(false)}
          onAnimationComplete={() => setHidden(!isOpen)}
          animate={{ width: isOpen ? '100%' : 0 }}
          style={{
            overflow: 'hidden',
            whiteSpace: 'nowrap',
          }}
        >
          <ToolboxNodes />
        </motion.div>
        <DraftStatus />
      </HStack>
    </Panel>
  )
}

export default DesignerToolbox
