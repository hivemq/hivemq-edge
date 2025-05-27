import { useContext } from 'react'
import type { EdgeFlowContextType } from './FlowContext.tsx'
import { EdgeFlowContext } from './FlowContext.tsx'

export const useEdgeFlowContext = () => {
  const context = useContext<EdgeFlowContextType | null>(EdgeFlowContext)
  if (context === null) {
    throw Error('useEdgeFlowContext must be used within a EdgeFlowContext')
  }
  return context
}
