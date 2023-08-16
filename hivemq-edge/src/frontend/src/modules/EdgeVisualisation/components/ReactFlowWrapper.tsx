import ReactFlow, { Background, Controls } from 'reactflow'
import 'reactflow/dist/style.css'

import useGetFlowElements from '../hooks/useGetFlowElements.tsx'

const ReactFlowWrapper = () => {
  const { nodes, edges, onNodesChange, onEdgesChange } = useGetFlowElements()

  return (
    <ReactFlow
      deleteKeyCode={null}
      snapToGrid={true}
      nodes={nodes}
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
