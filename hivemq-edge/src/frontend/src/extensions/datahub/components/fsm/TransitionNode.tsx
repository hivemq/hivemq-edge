import type { FC } from 'react'
import type { NodeProps, Node } from '@xyflow/react'
import { Handle, Position } from '@xyflow/react'
import { Checkbox, HStack, Tag } from '@chakra-ui/react'

export const TransitionNode: FC<NodeProps<Node<{ label: string }>>> = (props) => {
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
