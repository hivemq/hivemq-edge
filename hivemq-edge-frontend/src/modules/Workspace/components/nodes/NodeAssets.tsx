import type { FC } from 'react'
import { useMemo } from 'react'
import type { NodeProps } from '@xyflow/react'
import { Handle, Position } from '@xyflow/react'
import { useTranslation } from 'react-i18next'
import { Icon, Text, useColorModeValue, VStack } from '@chakra-ui/react'

import type { ManagedAsset } from '@/api/__generated__'
import { AssetMapping } from '@/api/__generated__'
import { useListManagedAssets } from '@/api/hooks/usePulse/useListManagedAssets.ts'

import LoaderSpinner from '@/components/Chakra/LoaderSpinner.tsx'
import { HqAssets } from '@/components/Icons'
import { SelectEntityType } from '@/components/MQTT/types.ts'
import MappingBadge from '@/modules/Workspace/components/parts/MappingBadge.tsx'
import type { NodeAssetsType } from '@/modules/Workspace/types.ts'
import { useContextMenu } from '@/modules/Workspace/hooks/useContextMenu.ts'
import ContextualToolbar from '@/modules/Workspace/components/nodes/ContextualToolbar.tsx'
import NodeWrapper from '@/modules/Workspace/components/parts/NodeWrapper.tsx'
import { CONFIG_ADAPTER_WIDTH } from '@/modules/Workspace/utils/nodes-utils.ts'

const NodeAssets: FC<NodeProps<NodeAssetsType>> = ({ id, data, selected, dragging }) => {
  const { t } = useTranslation()
  const bgColour = useColorModeValue('gray.300', 'gray.900')
  const { data: allAssets, isLoading } = useListManagedAssets()

  const mappedAssets = useMemo<ManagedAsset[]>(() => {
    if (!allAssets?.items) return []
    return allAssets.items.filter((asset) => asset.mapping?.status === AssetMapping.status.STREAMING)
  }, [allAssets])

  const { onContextMenu } = useContextMenu(id, selected, `/workspace/node/pulse-assets`)

  return (
    <>
      <ContextualToolbar id={id} title={data.label} dragging={dragging} onOpenPanel={onContextMenu}></ContextualToolbar>
      <NodeWrapper
        isSelected={selected}
        onDoubleClick={onContextMenu}
        onContextMenu={onContextMenu}
        wordBreak="break-word"
        textAlign="center"
        borderTopRadius={30}
        borderBottomRadius={30}
        flexDirection="row"
        p={0}
        w={CONFIG_ADAPTER_WIDTH}
        alignItems="center"
        h={120}
      >
        <VStack
          h="100%"
          p={4}
          backgroundColor={bgColour}
          borderTopLeftRadius={30}
          borderBottomLeftRadius={30}
          justifyContent="center"
        >
          <Icon as={HqAssets} boxSize={10} />
        </VStack>
        <VStack p={2} h="100%" justifyContent="space-evenly">
          <Text data-testid="assets-description" noOfLines={1}>
            {t('pulse.mapper.title')}
          </Text>
          {isLoading && <LoaderSpinner />}
          {!isLoading && <MappingBadge destinations={mappedAssets.map((e) => e.topic)} type={SelectEntityType.TOPIC} />}
        </VStack>
      </NodeWrapper>
      <Handle type="source" position={Position.Bottom} id="Bottom" isConnectable={false} />
      <Handle type="source" position={Position.Top} id="Top" isConnectable={false} />
    </>
  )
}

export default NodeAssets
