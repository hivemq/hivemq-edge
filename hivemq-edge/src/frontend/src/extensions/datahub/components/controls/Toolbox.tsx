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
            data-testid="toolbox-trigger"
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
          <HStack
            pb={2}
            gap={5}
            role="group"
            aria-label={t('workspace.toolbox.aria-label') as string}
            backgroundColor="var(--chakra-colors-chakra-body-bg)"
          >
            <ButtonGroup variant="outline" size="sm" aria-labelledby="group-pipeline">
              <VStack pl={2} alignItems="flex-start">
                <Text id="group-pipeline">{t('workspace.toolbox.group.pipeline')}</Text>
                <HStack>
                  <Tool nodeType={DataHubNodeType.TOPIC_FILTER} />
                  <Tool nodeType={DataHubNodeType.CLIENT_FILTER} />
                </HStack>
              </VStack>
            </ButtonGroup>
            <ButtonGroup variant="outline" size="sm" aria-labelledby="group-dataPolicy">
              <VStack alignItems="flex-start">
                <Text id="group-dataPolicy">{t('workspace.toolbox.group.dataPolicy')}</Text>
                <HStack>
                  <Tool nodeType={DataHubNodeType.DATA_POLICY} />
                  <Tool nodeType={DataHubNodeType.VALIDATOR} />
                  <Tool nodeType={DataHubNodeType.SCHEMA} />{' '}
                </HStack>
              </VStack>
            </ButtonGroup>
            <ButtonGroup variant="outline" size="sm" aria-labelledby="group-behaviorPolicy">
              <VStack alignItems="flex-start">
                <Text id="group-behaviorPolicy">{t('workspace.toolbox.group.behaviorPolicy')}</Text>
                <HStack>
                  <Tool nodeType={DataHubNodeType.BEHAVIOR_POLICY} />
                  <Tool nodeType={DataHubNodeType.TRANSITION} />
                </HStack>
              </VStack>
            </ButtonGroup>
            <ButtonGroup variant="outline" size="sm" aria-labelledby="group-operation">
              <VStack alignItems="flex-start" pr={2}>
                <Text id="group-operation">{t('workspace.toolbox.group.operation')}</Text>
                <HStack>
                  <Tool nodeType={DataHubNodeType.OPERATION} />
                  <Tool nodeType={DataHubNodeType.FUNCTION} />
                </HStack>
              </VStack>
            </ButtonGroup>
          </HStack>
        </motion.div>
      </HStack>
    </Panel>
  )
}
