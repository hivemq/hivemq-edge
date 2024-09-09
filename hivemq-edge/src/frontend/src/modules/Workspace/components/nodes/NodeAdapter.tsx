import { FC, useMemo } from 'react'
import { Handle, NodeProps, Position, useStore } from 'reactflow'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router-dom'
import { Box, HStack, Icon, Image, SkeletonText, Text, VStack } from '@chakra-ui/react'

import { type Adapter } from '@/api/__generated__'
import { useGetAdapterTypes } from '@/api/hooks/useProtocolAdapters/useGetAdapterTypes.ts'
import IconButton from '@/components/Chakra/IconButton.tsx'
import { ConnectionStatusBadge } from '@/components/ConnectionStatusBadge'
import { type TopicFilter } from '@/modules/Workspace/types.ts'
import { useEdgeFlowContext } from '@/modules/Workspace/hooks/useEdgeFlowContext.ts'
import { discoverAdapterTopics } from '@/modules/Workspace/utils/topics-utils.ts'
import { useContextMenu } from '@/modules/Workspace/hooks/useContextMenu.ts'
import { deviceCapabilityIcon, isBidirectional } from '@/modules/Workspace/utils/adapter.utils.ts'
import ContextualToolbar from '@/modules/Workspace/components/nodes/ContextualToolbar.tsx'
import NodeWrapper from '@/modules/Workspace/components/parts/NodeWrapper.tsx'
import TopicsContainer from '@/modules/Workspace/components/parts/TopicsContainer.tsx'
import { CONFIG_ADAPTER_WIDTH } from '@/modules/Workspace/utils/nodes-utils.ts'
import WorkspaceButtonGroup from '@/modules/Workspace/components/parts/WorkspaceButtonGroup.tsx'
import { selectorIsSkeletonZoom } from '@/modules/Workspace/utils/react-flow.utils.ts'

const NodeAdapter: FC<NodeProps<Adapter>> = ({ id, data: adapter, selected }) => {
  const { t } = useTranslation()
  const { data: protocols } = useGetAdapterTypes()
  const adapterProtocol = protocols?.items?.find((e) => e.id === adapter.type)
  const { options } = useEdgeFlowContext()
  const topics = useMemo<TopicFilter[]>(() => {
    if (!adapterProtocol) return []
    if (!adapter.config) return []
    return discoverAdapterTopics(adapterProtocol, adapter.config).map((e) => ({ topic: e }))
  }, [adapter.config, adapterProtocol])
  const { onContextMenu } = useContextMenu(id, selected, `/workspace/node/adapter/${adapter.type}`)
  const navigate = useNavigate()
  const showSkeleton = useStore(selectorIsSkeletonZoom)

  const HACK_BIDIRECTIONAL = isBidirectional(adapterProtocol)
  const adapterNavPath = `/workspace/node/adapter/${adapter.type}/${id}`

  return (
    <>
      <ContextualToolbar id={id} onOpenPanel={onContextMenu}>
        <WorkspaceButtonGroup>
          {HACK_BIDIRECTIONAL && (
            <IconButton
              icon={<Icon as={deviceCapabilityIcon['WRITE']} />}
              aria-label={t('workspace.toolbar.command.subscriptions.outward')}
              onClick={() => navigate(`${adapterNavPath}/outward`)}
            />
          )}
          <IconButton
            icon={<Icon as={deviceCapabilityIcon['READ']} />}
            aria-label={t('workspace.toolbar.command.subscriptions.inward')}
            onClick={() => navigate(`${adapterNavPath}/inward`)}
          />
        </WorkspaceButtonGroup>
      </ContextualToolbar>
      <NodeWrapper
        isSelected={selected}
        onDoubleClick={onContextMenu}
        onContextMenu={onContextMenu}
        w={CONFIG_ADAPTER_WIDTH}
        p={2}
      >
        {!showSkeleton && (
          <VStack>
            {HACK_BIDIRECTIONAL && <TopicsContainer topics={[{ topic: 'topic/mock/todo' }]} />}

            <HStack>
              <Image aria-label={adapter.type} boxSize="20px" objectFit="scale-down" src={adapterProtocol?.logoUrl} />
              <Text flex={1} data-testid="adapter-node-name">
                {adapter.id}
              </Text>
            </HStack>
            {options.showStatus && (
              <Box flex={1}>
                <ConnectionStatusBadge status={adapter.status} />
              </Box>
            )}
            {options.showTopics && <TopicsContainer topics={topics} />}
          </VStack>
        )}
        {showSkeleton && (
          <HStack px={6} my="4">
            <Box>
              <ConnectionStatusBadge status={adapter.status} skeleton />
            </Box>
            <Box w="100%">
              <SkeletonText
                noOfLines={topics.length ? 2 : 1}
                spacing="4"
                skeletonHeight="2"
                startColor="gray.500"
                endColor="gray.500"
                aria-label={adapter.id}
              />
            </Box>
          </HStack>
        )}
      </NodeWrapper>
      <Handle type="source" position={Position.Bottom} id="Bottom" isConnectable={false} />
      {HACK_BIDIRECTIONAL && <Handle type="source" position={Position.Top} id="Top" isConnectable={false} />}
    </>
  )
}

export default NodeAdapter
