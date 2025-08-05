import type { FC } from 'react'
import { HStack, Icon, Text, VStack } from '@chakra-ui/react'
import type { NodeProps } from '@xyflow/react'
import { Handle, Position } from '@xyflow/react'
import { useTranslation } from 'react-i18next'

import { Capability } from '@/api/__generated__'
import { useGetCapability } from '@/api/hooks/useFrontendServices/useGetCapability.ts'
import { HqPulseActivated, HqPulseNotActivated, PulseAgentIcon } from '@/components/Icons'
import ContextualToolbar from '@/modules/Workspace/components/nodes/ContextualToolbar.tsx'
import NodeWrapper from '@/modules/Workspace/components/parts/NodeWrapper.tsx'
import { CONFIG_ADAPTER_WIDTH } from '@/modules/Workspace/utils/nodes-utils.ts'
import type { NodePulseType } from '@/modules/Workspace/types'

const NodePulse: FC<NodeProps<NodePulseType>> = ({ id, data, selected, dragging }) => {
  const { t } = useTranslation()
  const { data: hasPulseCapability, isSuccess } = useGetCapability(Capability.id.PULSE_ASSET_MANAGEMENT)

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
        <HStack w="100%" justifyContent="flex-end" gap={1} data-testid="pulse-client-capability">
          {isSuccess && (
            <Icon
              boxSize={4}
              as={hasPulseCapability ? HqPulseActivated : HqPulseNotActivated}
              data-type="pulse-client-capability"
            />
          )}
        </HStack>
        <VStack>
          <HStack w="100%" data-testid="device-description">
            <PulseAgentIcon boxSize={10} />
            <Text>{t('pulse.client.title')}</Text>
          </HStack>
        </VStack>
      </NodeWrapper>
      <Handle type="target" position={Position.Bottom} isConnectable={false} />
    </>
  )
}

export default NodePulse
