import type { FC } from 'react'
import type { NodeProps } from 'reactflow'
import { Handle, Position, useStore } from 'reactflow'
import { Box, HStack, Image, SkeletonText, Text, VStack } from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'

import type { Bridge } from '@/api/__generated__'
import logo from '@/assets/hivemq/05-icon-hivemq-bridge-extension.svg'

import { SelectEntityType } from '@/components/MQTT/types'
import { ConnectionStatusBadge } from '@/components/ConnectionStatusBadge'
import ContextualToolbar from '@/modules/Workspace/components/nodes/ContextualToolbar.tsx'
import { CONFIG_ADAPTER_WIDTH } from '@/modules/Workspace/utils/nodes-utils.ts'
import { selectorIsSkeletonZoom } from '@/modules/Workspace/utils/react-flow.utils.ts'
import { getBridgeTopics } from '@/modules/Workspace/utils/topics-utils.ts'
import { useEdgeFlowContext } from '@/modules/Workspace/hooks/useEdgeFlowContext.ts'
import { useContextMenu } from '@/modules/Workspace/hooks/useContextMenu.ts'

import NodeWrapper from '../parts/NodeWrapper.tsx'
import MappingBadge from '../parts/MappingBadge.tsx'

const NodeBridge: FC<NodeProps<Bridge>> = ({ id, selected, data: bridge, dragging }) => {
  const { t } = useTranslation()
  const topics = getBridgeTopics(bridge)
  const { options } = useEdgeFlowContext()
  const { onContextMenu } = useContextMenu(id, selected, '/workspace/node/bridge')
  const showSkeleton = useStore(selectorIsSkeletonZoom)

  return (
    <>
      <ContextualToolbar id={id} title={bridge.id} dragging={dragging} onOpenPanel={onContextMenu} />
      <NodeWrapper
        isSelected={selected}
        onDoubleClick={onContextMenu}
        onContextMenu={onContextMenu}
        p={3}
        w={CONFIG_ADAPTER_WIDTH}
      >
        {!showSkeleton && (
          <VStack>
            {options.showTopics && (
              <MappingBadge destinations={topics.remote.map((filter) => filter.topic)} type={SelectEntityType.TOPIC} />
            )}
            <HStack>
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
            {options.showTopics && (
              <MappingBadge destinations={topics.local.map((filter) => filter.topic)} type={SelectEntityType.TOPIC} />
            )}
          </VStack>
        )}
        {showSkeleton && (
          <HStack px={6} my="4">
            <Box>
              <ConnectionStatusBadge status={bridge.status} skeleton />
            </Box>
            <Box w="100%">
              <SkeletonText
                noOfLines={[...topics.local, ...topics.remote].length ? 2 : 1}
                spacing="4"
                skeletonHeight="2"
                startColor="gray.500"
                endColor="gray.500"
                aria-label={bridge.id}
              />
            </Box>
          </HStack>
        )}
      </NodeWrapper>
      <Handle type="source" position={Position.Top} id="Top" isConnectable={false} />
      <Handle type="source" position={Position.Bottom} id="Bottom" isConnectable={false} />
    </>
  )
}

export default NodeBridge
