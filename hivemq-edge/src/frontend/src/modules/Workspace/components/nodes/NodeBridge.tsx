import { FC } from 'react'
import { Handle, NodeProps, Position } from 'reactflow'
import { Box, HStack, Image, Text, VStack } from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'

import { Bridge } from '@/api/__generated__'
import { ConnectionStatusBadge } from '@/components/ConnectionStatusBadge'
import logo from '@/assets/hivemq/05-icon-hivemq-bridge-extension.svg'

import NodeWrapper from '../parts/NodeWrapper.tsx'
import TopicsContainer from '../parts/TopicsContainer.tsx'
import { getBridgeTopics } from '../../utils/topics-utils.ts'
import { useEdgeFlowContext } from '../../hooks/useEdgeFlowContext.ts'
import { useContextMenu } from '../../hooks/useContextMenu.ts'
import ContextualToolbar from '@/modules/Workspace/components/nodes/ContextualToolbar.tsx'

const NodeBridge: FC<NodeProps<Bridge>> = ({ id, selected, data: bridge }) => {
  const { t } = useTranslation()
  const topics = getBridgeTopics(bridge)
  const { options } = useEdgeFlowContext()
  const { onContextMenu } = useContextMenu(id, selected, '/workspace/node/bridge')

  return (
    <>
      <ContextualToolbar id={id} onOpenPanel={onContextMenu} />
      <NodeWrapper isSelected={selected} onDoubleClick={onContextMenu} onContextMenu={onContextMenu} p={3}>
        <VStack>
          {options.showTopics && <TopicsContainer topics={topics.remote} />}

          <HStack w="100%">
            <Image boxSize="20px" objectFit="scale-down" src={logo} alt={t('workspace.node.bridge')} />
            <Text flex={1} data-testid="bridge-node-name">
              {bridge.id}
            </Text>
          </HStack>
          {options.showStatus && (
            <Box flex={1}>
              <ConnectionStatusBadge status={bridge.status} />
            </Box>
          )}
          {options.showTopics && <TopicsContainer topics={topics.local} />}
        </VStack>
      </NodeWrapper>
      <Handle type="source" position={Position.Top} id="Top" isConnectable={false} />
      <Handle type="source" position={Position.Bottom} id="Bottom" isConnectable={false} />
    </>
  )
}

export default NodeBridge
