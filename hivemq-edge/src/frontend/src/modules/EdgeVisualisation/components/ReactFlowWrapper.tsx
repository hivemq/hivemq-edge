import { useMemo } from 'react'
import ReactFlow, { Background, Controls } from 'reactflow'
import 'reactflow/dist/style.css'

import useGetFlowElements from '../hooks/useGetFlowElements.tsx'
import { NodeTypes } from '../types.ts'
import NodeEdge from './NodeEdge.tsx'
import NodeAdapter from './NodeAdapter.tsx'

const ReactFlowWrapper = () => {
  const { nodes, edges, onNodesChange, onEdgesChange } = useGetFlowElements()
  const nodeTypes = useMemo(
    () => ({
      [NodeTypes.EDGE_NODE]: NodeEdge,
      [NodeTypes.ADAPTER_NODE]: NodeAdapter,
    }),
    []
  )
  return (
    <ReactFlow
      deleteKeyCode={null}
      snapToGrid={true}
      nodes={nodes}
      nodeTypes={nodeTypes}
      onNodesChange={onNodesChange}
      edges={edges}
      onEdgesChange={onEdgesChange}
    >
      <Background />
      <Controls />
    </ReactFlow>
  )
}

ReactFlowWrapper.propTypes = {}

export default ReactFlowWrapper
