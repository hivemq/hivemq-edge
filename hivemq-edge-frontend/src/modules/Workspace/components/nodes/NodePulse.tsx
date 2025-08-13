import type { FC } from 'react'
import { useMemo } from 'react'
import { Badge, HStack, Icon, Text, VStack } from '@chakra-ui/react'
import type { NodeProps } from '@xyflow/react'
import { Handle, Position } from '@xyflow/react'
import { useTranslation } from 'react-i18next'

import { AssetMapping, Capability, type ManagedAsset, PulseStatus } from '@/api/__generated__'
import { useGetCapability } from '@/api/hooks/useFrontendServices/useGetCapability.ts'
import { useListManagedAssets } from '@/api/hooks/usePulse/useListManagedAssets.ts'

import { HqPulseActivated, HqPulseNotActivated, PulseAgentIcon } from '@/components/Icons'
import LoaderSpinner from '@/components/Chakra/LoaderSpinner.tsx'
import { SelectEntityType } from '@/components/MQTT/types.ts'
import ContextualToolbar from '@/modules/Workspace/components/nodes/ContextualToolbar.tsx'
import NodeWrapper from '@/modules/Workspace/components/parts/NodeWrapper.tsx'
import { CONFIG_ADAPTER_WIDTH } from '@/modules/Workspace/utils/nodes-utils.ts'
import type { NodePulseType } from '@/modules/Workspace/types'
import MappingBadge from '@/modules/Workspace/components/parts/MappingBadge.tsx'

const NodePulse: FC<NodeProps<NodePulseType>> = ({ id, data, selected, dragging }) => {
  const { t } = useTranslation()
  const { data: hasPulseCapability, isSuccess } = useGetCapability(Capability.id.PULSE_ASSET_MANAGEMENT)
  const { data: allAssets, isLoading } = useListManagedAssets()

  const unmappedAssets = useMemo<ManagedAsset[]>(() => {
    if (!allAssets?.items) return []
    return allAssets.items.filter(
      (asset) => asset.mapping === undefined || asset.mapping?.status === AssetMapping.status.UNMAPPED
    )
  }, [allAssets])

  const assetStats = useMemo(() => {
    if (!allAssets?.items) return { unmapped: 0, mapped: 0 }
    if (!unmappedAssets) return { unmapped: 0, mapped: 0 }
    return { mapped: allAssets.items.length - unmappedAssets.length, unmapped: unmappedAssets.length }
  }, [allAssets?.items, unmappedAssets])

  const pulseStatus: PulseStatus.activationStatus = hasPulseCapability
    ? PulseStatus.activationStatus.ACTIVATED
    : PulseStatus.activationStatus.DEACTIVATED

  return (
    <>
      <ContextualToolbar id={id} title={data.label} dragging={dragging} hasNoOverview />
      <NodeWrapper
        isSelected={selected}
        // onDoubleClick={onContextMenu}
        // onContextMenu={onContextMenu}
        p={3}
        w={CONFIG_ADAPTER_WIDTH}
        borderTopRadius={30}
      >
        <HStack w="100%" justifyContent="flex-end" gap={1} data-testid="pulse-client-capabilities">
          <Badge colorScheme="teal">{assetStats.unmapped}</Badge>
          <Badge colorScheme="gray">{assetStats.mapped}</Badge>
          {isSuccess && (
            <Icon
              boxSize={4}
              as={hasPulseCapability ? HqPulseActivated : HqPulseNotActivated}
              data-type={pulseStatus}
              aria-label={t('pulse.workspace.nodeClient.status', { context: pulseStatus })}
            />
          )}
        </HStack>
        <VStack>
          <HStack w="100%" data-testid="pulse-client-description">
            <PulseAgentIcon boxSize={10} />
            <Text>{t('pulse.workspace.nodeClient.title')}</Text>
          </HStack>
          {isLoading && <LoaderSpinner />}
          {!isLoading && (
            <MappingBadge destinations={unmappedAssets.map((e) => e.topic)} type={SelectEntityType.PULSE_ASSET} />
          )}
        </VStack>
      </NodeWrapper>
      <Handle type="target" position={Position.Bottom} isConnectable={false} />
      <Handle type="source" position={Position.Bottom} isConnectable={false} />
    </>
  )
}

export default NodePulse
