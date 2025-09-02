import type { FC } from 'react'
import { useMemo } from 'react'
import { HStack, Text, VStack } from '@chakra-ui/react'
import type { NodeProps } from '@xyflow/react'
import { Handle, Position } from '@xyflow/react'
import { useTranslation } from 'react-i18next'

import { AssetMapping, Capability, type ManagedAsset, PulseStatus } from '@/api/__generated__'
import { useGetCapability } from '@/api/hooks/useFrontendServices/useGetCapability.ts'
import { useListManagedAssets } from '@/api/hooks/usePulse/useListManagedAssets.ts'

import TooltipBadge from '@/components/Chakra/TooltipBadge.tsx'
import TooltipIcon from '@/components/Chakra/TooltipIcon.tsx'
import LoaderSpinner from '@/components/Chakra/LoaderSpinner.tsx'
import { HqPulseActivated, HqPulseNotActivated, PulseAgentIcon } from '@/components/Icons'
import { SelectEntityType } from '@/components/MQTT/types.ts'
import PulseStatusBadge from '@/modules/Pulse/components/activation/PulseStatusBadge.tsx'
import ContextualToolbar from '@/modules/Workspace/components/nodes/ContextualToolbar.tsx'
import NodeWrapper from '@/modules/Workspace/components/parts/NodeWrapper.tsx'
import MappingBadge from '@/modules/Workspace/components/parts/MappingBadge.tsx'
import { useContextMenu } from '@/modules/Workspace/hooks/useContextMenu.ts'
import { CONFIG_ADAPTER_WIDTH } from '@/modules/Workspace/utils/nodes-utils.ts'
import type { NodePulseType } from '@/modules/Workspace/types'

const NodePulse: FC<NodeProps<NodePulseType>> = ({ id, data, selected, dragging }) => {
  const { t } = useTranslation()
  const { data: hasPulseCapability, isSuccess } = useGetCapability(Capability.id.PULSE_ASSET_MANAGEMENT)
  const { data: allAssets, isLoading } = useListManagedAssets()

  const unmappedAssets = useMemo<ManagedAsset[]>(() => {
    if (!allAssets?.items) return []
    return allAssets.items.filter((asset) => asset.mapping.status === AssetMapping.status.UNMAPPED)
  }, [allAssets])

  const assetStats = useMemo(() => {
    if (!allAssets?.items) return { unmapped: 0, mapped: 0 }
    if (!unmappedAssets) return { unmapped: 0, mapped: 0 }
    return { mapped: allAssets.items.length - unmappedAssets.length, unmapped: unmappedAssets.length }
  }, [allAssets?.items, unmappedAssets])

  const { onContextMenu } = useContextMenu(id, selected, `/workspace/node/pulse-assets`)

  const pulseStatus: PulseStatus.activation = hasPulseCapability
    ? PulseStatus.activation.ACTIVATED
    : PulseStatus.activation.DEACTIVATED

  return (
    <>
      <ContextualToolbar id={id} title={data.label} dragging={dragging} onOpenPanel={onContextMenu} />
      <NodeWrapper
        isSelected={selected}
        onDoubleClick={onContextMenu}
        onContextMenu={onContextMenu}
        p={3}
        w={CONFIG_ADAPTER_WIDTH}
        borderTopRadius={30}
      >
        <HStack w="100%" justifyContent="flex-end" gap={1} data-testid="pulse-client-capabilities" role="status">
          <TooltipBadge
            aria-label={t('pulse.coverage.unmapped')}
            data-testid="pulse-client-unmapped"
            colorScheme="teal"
          >
            {assetStats.unmapped}
          </TooltipBadge>
          <TooltipBadge data-testid="pulse-client-mapped" colorScheme="gray" aria-label={t('pulse.coverage.mapped')}>
            {assetStats.mapped}
          </TooltipBadge>
          {isSuccess && (
            <TooltipIcon
              data-testid="pulse-client-mapped"
              aria-label={t('pulse.workspace.nodeClient.status', { context: pulseStatus })}
              as={hasPulseCapability ? HqPulseActivated : HqPulseNotActivated}
              data-type={pulseStatus}
            />
          )}
        </HStack>
        <VStack>
          <HStack w="100%" data-testid="pulse-client-description">
            <PulseAgentIcon boxSize={10} />
            <Text>{t('pulse.workspace.nodeClient.title')}</Text>
          </HStack>
          {data.status && <PulseStatusBadge status={data.status} />}
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
