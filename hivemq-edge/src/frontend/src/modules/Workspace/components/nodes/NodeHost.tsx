import type { FC } from 'react'
import type { NodeProps } from 'reactflow'
import { Handle, Position } from 'reactflow'
import { Text } from '@chakra-ui/react'

import NodeWrapper from '@/modules/Workspace/components/parts/NodeWrapper.tsx'
import { CONFIG_ADAPTER_WIDTH } from '@/modules/Workspace/utils/nodes-utils.ts'

const NodeHost: FC<NodeProps> = ({ selected, data }) => {
  const { label } = data
  return (
    <>
      <NodeWrapper
        isSelected={selected}
        wordBreak="break-word"
        textAlign="center"
        p={3}
        w={CONFIG_ADAPTER_WIDTH}
        borderBottomRadius={30}
      >
        <Text pb={5}>{label}</Text>
      </NodeWrapper>
      <Handle type="target" position={Position.Top} isConnectable={false} />
    </>
  )
}

export default NodeHost
