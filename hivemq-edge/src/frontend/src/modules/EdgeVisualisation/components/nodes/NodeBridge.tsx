import { FC } from 'react'
import { Handle, Position, NodeProps } from 'reactflow'
import { Box, HStack, Image, Text, VStack } from '@chakra-ui/react'

import { Bridge } from '@/api/__generated__'
import { ConnectionStatusBadge } from '@/components/ConnectionStatusBadge'
import logo from '@/assets/hivemq/05-icon-hivemq-bridge-extension.svg'

import NodeWrapper from '../parts/NodeWrapper.tsx'

const NodeBridge: FC<NodeProps<Bridge>> = ({ data: bridge }) => {
  return (
    <>
      <NodeWrapper p={3}>
        <VStack>
          <HStack w={'100%'}>
            <Image boxSize="20px" objectFit="scale-down" src={logo} />
            <Text flex={1}>{bridge.id} </Text>
          </HStack>
          <Box flex={1}>
            <ConnectionStatusBadge status={bridge.bridgeRuntimeInformation?.connectionStatus?.status} />
          </Box>
        </VStack>
      </NodeWrapper>
      <Handle type="source" position={Position.Top} id="Top" isConnectable={true} />
    </>
  )
}

export default NodeBridge
