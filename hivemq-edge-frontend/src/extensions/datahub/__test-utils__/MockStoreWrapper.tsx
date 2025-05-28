import type { FC } from 'react'
import { useEffect } from 'react'

import useDataHubDraftStore from '../hooks/useDataHubDraftStore.ts'
import type { PolicyCheckState, WorkspaceState, WorkspaceStatus } from '../types.ts'
import { usePolicyChecksStore } from '@datahub/hooks/usePolicyChecksStore.ts'
import { ReactFlowProvider } from '@xyflow/react'

type Optional<T, K extends keyof T> = Pick<Partial<T>, K> & Omit<T, K>
interface MockStoreWrapperConfig {
  initialState?: Optional<WorkspaceState & WorkspaceStatus, 'nodes' | 'edges' | 'status' | 'name' | 'type'>
}

interface MockStoreWrapperProps {
  children: React.ReactNode
  config: MockStoreWrapperConfig
}

export const MockStoreWrapper: FC<MockStoreWrapperProps> = ({ config, children }) => {
  const { onAddNodes, onAddEdges, reset, setStatus } = useDataHubDraftStore()

  useEffect(() => {
    reset()
  }, [reset])

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
    if (initialState?.status) {
      setStatus(initialState?.status)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  return <>{children}</>
}

interface MockChecksStoreWrapperProps {
  children: React.ReactNode
  config: Optional<PolicyCheckState, 'node' | 'status' | 'report'>
}

export const MockChecksStoreWrapper: FC<MockChecksStoreWrapperProps> = ({ config, children }) => {
  const { setNode, setReport } = usePolicyChecksStore()
  const { onAddNodes, reset } = useDataHubDraftStore()

  useEffect(() => {
    reset()
  }, [reset])

  useEffect(() => {
    const { node, report } = config
    if (node) {
      onAddNodes([{ item: node, type: 'add' }])
      setNode(node)
    }

    if (report) setReport(report)
  }, [config, onAddNodes, setNode, setReport])

  return <ReactFlowProvider>{children}</ReactFlowProvider>
}
