import { FC } from 'react'
import { Handle, Position, NodeProps } from 'reactflow'
import { Box, HStack, Image, Text, VStack } from '@chakra-ui/react'

import { Bridge } from '@/api/__generated__'
import { ConnectionStatusBadge } from '@/components/ConnectionStatusBadge'
import logo from '@/assets/app/bridges.svg'

import GenericNode from './GenericNode.tsx'

const NodeBridge: FC<NodeProps<Bridge>> = ({ data: bridge }) => {
  return (
    <>
      <GenericNode p={3}>
        <VStack>
          <HStack w={'100%'}>
            <Image boxSize="20px" objectFit="scale-down" src={logo} />
            <Text flex={1}>{bridge.id} </Text>
          </HStack>
          <Box flex={1}>
            <ConnectionStatusBadge status={bridge.bridgeRuntimeInformation?.connectionStatus?.status} />
          </Box>
        </VStack>
      </GenericNode>
      <Handle type="source" position={Position.Top} id="Top" isConnectable={true} />
    </>
  )
}

export default NodeBridge
