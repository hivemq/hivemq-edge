import { FC, useEffect } from 'react'

import useDataHubDraftStore from '../hooks/useDataHubDraftStore.ts'
import { WorkspaceState } from '../types.ts'

type Optional<T, K extends keyof T> = Pick<Partial<T>, K> & Omit<T, K>
interface MockStoreWrapperConfig {
  initialState?: Optional<WorkspaceState, 'nodes' | 'edges' | 'functions'>
}

interface MockStoreWrapperProps {
  children: React.ReactNode
  config: MockStoreWrapperConfig
}

export const MockStoreWrapper: FC<MockStoreWrapperProps> = ({ config, children }) => {
  const { onAddNodes, onAddEdges } = useDataHubDraftStore()

  useEffect(() => {
    const { initialState } = config
    if (initialState?.nodes)
      onAddNodes(
        initialState.nodes.map((n) => ({
          item: n,
          type: 'add',
        }))
      )
    if (initialState?.edges)
      onAddEdges(
        initialState.edges.map((e) => ({
          item: e,
          type: 'add',
        }))
      )
  }, [config, onAddEdges, onAddNodes])

  return <>{children}</>
}
