import { CSSProperties, FC, HTMLAttributes, useMemo } from 'react'
import { getConnectedEdges, Handle, HandleProps, useNodeId } from 'reactflow'

import useDataHubDraftStore from '../../hooks/useDataHubDraftStore.ts'

interface CustomHandleProps extends Omit<HandleProps & Omit<HTMLAttributes<HTMLDivElement>, 'id'>, 'isConnectable'> {
  isConnectable?: boolean | number
}

export const CustomHandle: FC<CustomHandleProps> = (props) => {
  const { nodes, edges } = useDataHubDraftStore()
  const nodeId = useNodeId()

  const isHandleConnectable = useMemo(() => {
    if (typeof props.isConnectable === 'number') {
      const node = nodes.find((node) => node.id === nodeId)
      if (node) {
        const connectedEdges = getConnectedEdges([node], edges)

        const toHandle = connectedEdges.filter((edge) => {
          const otherEnd = props.type === 'source' ? edge.sourceHandle : edge.targetHandle
          return otherEnd === props.id
        })

        return toHandle.length < props.isConnectable
      }
      return false
    }
    return true
  }, [edges, nodeId, nodes, props.id, props.isConnectable, props.type])

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
