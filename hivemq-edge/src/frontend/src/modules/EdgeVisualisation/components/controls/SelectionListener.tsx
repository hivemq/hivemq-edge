import { useEffect } from 'react'
import { useLocation } from 'react-router-dom'
import { ReactFlowState, useStore } from 'reactflow'

const addSelectedNodesState = (state: ReactFlowState) => (nodeIds: string[]) => state.addSelectedNodes(nodeIds)

const SelectionListener = () => {
  const { state } = useLocation()
  const addSelectedNodes = useStore(addSelectedNodesState)

  useEffect(() => {
    const { selectedAdapter } = state || {}
    const { adapterId, type } = selectedAdapter || {}
    if (!adapterId || !type) return

    addSelectedNodes([`adapter@${adapterId}`])
  }, [addSelectedNodes, state])

  return null
}

export default SelectionListener
