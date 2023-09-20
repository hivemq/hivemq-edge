import { FC } from 'react'
import { Handle, Position, NodeProps } from 'reactflow'
import { Box, HStack, Image, Text, VStack } from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'

import { Bridge } from '@/api/__generated__'
import { ConnectionStatusBadge } from '@/components/ConnectionStatusBadge'
import logo from '@/assets/hivemq/05-icon-hivemq-bridge-extension.svg'

import NodeWrapper from '../parts/NodeWrapper.tsx'
import TopicsContainer from '../parts/TopicsContainer.tsx'
import { getBridgeTopics } from '../../utils/topics-utils.ts'
import { useEdgeFlowContext } from '../../hooks/useEdgeFlowContext.tsx'

const NodeBridge: FC<NodeProps<Bridge>> = ({ data: bridge }) => {
  const { t } = useTranslation()
  const topics = getBridgeTopics(bridge)
  const { options } = useEdgeFlowContext()

  return (
    <>
      <NodeWrapper p={3}>
        <VStack>
          {options.showTopics && <TopicsContainer topics={topics.remote} />}

          <HStack w={'100%'}>
            <Image boxSize="20px" objectFit="scale-down" src={logo} alt={t('workspace.node.bridge') as string} />
            <Text flex={1} data-testid={'bridge-node-name'}>
              {bridge.id}
            </Text>
          </HStack>
          {options.showStatus && (
            <Box flex={1}>
              <ConnectionStatusBadge status={bridge.status?.connection} />
            </Box>
          )}
          {options.showTopics && <TopicsContainer topics={topics.local} />}
        </VStack>
      </NodeWrapper>
      <Handle type="source" position={Position.Top} id="Top" isConnectable={true} />
      {options.showHosts && <Handle type="source" position={Position.Bottom} id="Bottom" isConnectable={true} />}
    </>
  )
}

export default NodeBridge
