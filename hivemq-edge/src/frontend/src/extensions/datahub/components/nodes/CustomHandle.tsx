import { CSSProperties, FC, HTMLAttributes, useMemo } from 'react'
import { Handle, HandleProps, useNodeId } from 'reactflow'

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
