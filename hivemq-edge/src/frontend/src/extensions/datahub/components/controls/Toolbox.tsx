import { motion } from 'framer-motion'
import { useState } from 'react'
import { Panel } from 'reactflow'
import { useTranslation } from 'react-i18next'
import { HStack, useDisclosure, Text, VStack, Box, IconButton, Icon, ButtonGroup } from '@chakra-ui/react'
import { FaAngleLeft, FaAngleRight, FaTools } from 'react-icons/fa'

import { DataHubNodeType } from '../../types.ts'
import Tool from './Tool.tsx'

export const Toolbox = () => {
  const { t } = useTranslation('datahub')
  const { getButtonProps, getDisclosureProps, isOpen } = useDisclosure()
  const [hidden, setHidden] = useState(!isOpen)

  return (
    <Panel position="top-left" style={{ margin: '5px' }}>
      <HStack alignItems="flex-start">
        <Box>
          <IconButton
            aria-label={t('workspace.toolbox.trigger', { context: !isOpen ? 'open' : 'close' })}
            icon={
              <>
                <Icon as={FaTools} />
                <Icon as={isOpen ? FaAngleLeft : FaAngleRight} />
              </>
            }
            {...getButtonProps()}
            px={2}
          />
        </Box>
        <motion.div
          {...getDisclosureProps()}
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
          <HStack
            pb={2}
            gap={5}
            role="group"
            aria-label={t('workspace.toolbox.aria-label') as string}
            backgroundColor="var(--chakra-colors-chakra-body-bg)"
          >
            <VStack pl={2} alignItems="flex-start">
              <Text id="group-pipeline">{t('workspace.toolbox.group.pipeline')}</Text>
              <ButtonGroup variant="outline" size="sm" aria-labelledby="group-pipeline">
                <Tool nodeType={DataHubNodeType.TOPIC_FILTER} />
                <Tool nodeType={DataHubNodeType.CLIENT_FILTER} />
              </ButtonGroup>
            </VStack>
            <VStack alignItems="flex-start">
              <Text id="group-dataPolicy">{t('workspace.toolbox.group.dataPolicy')}</Text>
              <ButtonGroup variant="outline" size="sm" aria-labelledby="group-dataPolicy">
                <Tool nodeType={DataHubNodeType.DATA_POLICY} />
                <Tool nodeType={DataHubNodeType.VALIDATOR} />
                <Tool nodeType={DataHubNodeType.SCHEMA} />{' '}
              </ButtonGroup>
            </VStack>
            <VStack alignItems="flex-start">
              <Text id="group-behaviorPolicy">{t('workspace.toolbox.group.behaviorPolicy')}</Text>
              <ButtonGroup variant="outline" size="sm" aria-labelledby="group-behaviorPolicy">
                <Tool nodeType={DataHubNodeType.BEHAVIOR_POLICY} />
                <Tool nodeType={DataHubNodeType.TRANSITION} />
              </ButtonGroup>
            </VStack>
            <VStack alignItems="flex-start" pr={2}>
              <Text id="group-action">{t('workspace.toolbox.group.action')}</Text>
              <ButtonGroup variant="outline" size="sm" aria-labelledby="group-action">
                <Tool nodeType={DataHubNodeType.OPERATION} />
              </ButtonGroup>
            </VStack>
          </HStack>
        </motion.div>
      </HStack>
    </Panel>
  )
}
