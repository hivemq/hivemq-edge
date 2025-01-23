import type { FC } from 'react'
import type { NodeProps } from 'reactflow'
import { Handle, Position } from 'reactflow'
import { Checkbox, HStack, Tag } from '@chakra-ui/react'

export const TransitionNode: FC<NodeProps> = (props) => {
  return (
    <>
      <Handle
        type="target"
        position={Position.Top}
        isConnectable={props.isConnectable}
        style={{ visibility: 'hidden', width: 0, height: 0, minWidth: 0, minHeight: 0, top: 0 }}
      />

      <HStack>
        <Tag>
          <Checkbox isChecked={false} isDisabled>
            {props.data.label}
          </Checkbox>
        </Tag>
      </HStack>
      <Handle
        type="source"
        position={Position.Bottom}
        isConnectable={false}
        style={{ visibility: 'hidden', width: 0, height: 0, minWidth: 0, minHeight: 0, top: 0 }}
      />
    </>
  )
}
