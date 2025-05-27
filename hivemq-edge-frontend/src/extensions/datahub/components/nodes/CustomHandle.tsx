import type { CSSProperties, FC, HTMLAttributes } from 'react'
import { useMemo } from 'react'
import type { HandleProps } from '@xyflow/react'
import { Handle, useNodeId } from '@xyflow/react'

import useDataHubDraftStore from '../../hooks/useDataHubDraftStore.ts'
import { isNodeHandleConnectable } from '@datahub/utils/node.utils.ts'

interface CustomHandleProps
  extends Omit<HandleProps & Pick<HTMLAttributes<HTMLDivElement>, 'style' | 'className'>, 'isConnectable'> {
  isConnectable?: boolean | number
}

export const CustomHandle: FC<CustomHandleProps> = (props) => {
  const { nodes, edges } = useDataHubDraftStore()
  const nodeId = useNodeId()

  const isHandleConnectable = useMemo(() => {
    const node = nodes.find((node) => node.id === nodeId)
    if (!node) return false
    return isNodeHandleConnectable(props, node, edges)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [nodes.length, props, edges, nodeId])

  let transform: CSSProperties = {
    width: '12px',
    height: '12px',
  }
  if (props.type === 'source') transform = { ...transform, borderRadius: 0 }
  if (props.position === 'left') transform = { ...transform, left: '-7px' }
  if (props.position === 'right') transform = { ...transform, right: '-7px' }
  if (props.position === 'top') transform = { ...transform, top: '-7px' }
  if (props.position === 'bottom') transform = { ...transform, bottom: '-7px' }

  return (
    <Handle
      {...props}
      isConnectable={isHandleConnectable}
      style={{
        ...transform,
        ...props.style,
      }}
    ></Handle>
  )
}
