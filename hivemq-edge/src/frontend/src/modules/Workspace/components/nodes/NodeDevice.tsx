import { FC, useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { Handle, Position, NodeProps, useStore } from 'reactflow'
import { HStack, Icon, Text, VStack } from '@chakra-ui/react'

import { useGetDomainTags } from '@/api/hooks/useProtocolAdapters/useGetDomainTags.ts'
import ToolbarButtonGroup from '@/components/react-flow/ToolbarButtonGroup.tsx'
import IconButton from '@/components/Chakra/IconButton.tsx'
import { PLCTagIcon } from '@/components/Icons/TopicIcon.tsx'
import { DeviceMetadata } from '@/modules/Workspace/types.ts'
import NodeWrapper from '@/modules/Workspace/components/parts/NodeWrapper.tsx'
import {
  deviceCapabilityIcon,
  deviceCategoryIcon,
  ProtocolAdapterCategoryName,
} from '@/modules/Workspace/utils/adapter.utils.ts'
import { useContextMenu } from '@/modules/Workspace/hooks/useContextMenu.ts'
import ContextualToolbar from '@/modules/Workspace/components/nodes/ContextualToolbar.tsx'
import { CONFIG_ADAPTER_WIDTH } from '@/modules/Workspace/utils/nodes-utils.ts'
import { selectorIsSkeletonZoom } from '@/modules/Workspace/utils/react-flow.utils.ts'
import MappingBadge from '@/modules/Workspace/components/parts/MappingBadge.tsx'

const NodeDevice: FC<NodeProps<DeviceMetadata>> = ({ id, selected, data, dragging }) => {
  const { t } = useTranslation()
  const { onContextMenu } = useContextMenu(id, selected, '/workspace/node')
  const { category, capabilities } = data
  const showSkeleton = useStore(selectorIsSkeletonZoom)
  const { data: deviceTags } = useGetDomainTags(data.sourceAdapterId)

  const tagNames = useMemo(() => {
    return deviceTags?.items?.map((tag) => tag.name) || []
  }, [deviceTags?.items])

  return (
    <>
      <ContextualToolbar id={id} title={data.protocol} onOpenPanel={onContextMenu} dragging={dragging} hasNoOverview>
        <ToolbarButtonGroup>
          <IconButton
            icon={<PLCTagIcon />}
            aria-label={t('workspace.toolbar.command.device.metadata')}
            onClick={onContextMenu}
          />
        </ToolbarButtonGroup>
      </ContextualToolbar>
      <NodeWrapper
        isSelected={selected}
        wordBreak="break-word"
        textAlign="center"
        p={3}
        w={CONFIG_ADAPTER_WIDTH}
        borderTopRadius={30}
      >
        <VStack>
          {!showSkeleton && (
            <>
              <HStack w="100%" justifyContent="flex-end" gap={1} data-testid="device-capabilities">
                {capabilities?.map((capability) => (
                  <Icon key={capability} boxSize={4} as={deviceCapabilityIcon[capability]} data-type={capability} />
                ))}
              </HStack>
              <HStack w="100%" data-testid="device-description">
                <Icon
                  as={deviceCategoryIcon[category?.name || ProtocolAdapterCategoryName.SIMULATION]}
                  data-type={category?.name}
                />
                <Text>{data.protocol}</Text>
              </HStack>
              <MappingBadge destinations={tagNames} isTag />
            </>
          )}
          {showSkeleton && (
            <Icon
              as={deviceCategoryIcon[category?.name || ProtocolAdapterCategoryName.SIMULATION]}
              data-type={category?.name}
              boxSize="14"
            />
          )}
        </VStack>
      </NodeWrapper>
      <Handle type="target" position={Position.Bottom} isConnectable={false} />
    </>
  )
}

export default NodeDevice
