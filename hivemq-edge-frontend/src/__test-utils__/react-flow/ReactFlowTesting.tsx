import { type FC, type ReactNode, useEffect } from 'react'
import { ReactFlowProvider } from '@xyflow/react'
import { Box, Card, CardBody, CardHeader, Tag } from '@chakra-ui/react'

import useWorkspaceStore from '@/modules/Workspace/hooks/useWorkspaceStore.ts'
import { type WorkspaceState } from '@/modules/Workspace/types.ts'
import { EdgeFlowProvider } from '@/modules/Workspace/hooks/EdgeFlowProvider.tsx'

type Optional<T, K extends keyof T> = Pick<Partial<T>, K> & Omit<T, K>
interface ReactFlowTestingConfig {
  initialState?: Optional<WorkspaceState, 'nodes' | 'edges' | 'layoutConfig' | 'isAutoLayoutEnabled' | 'layoutHistory'>
}

interface ReactFlowTestingProps {
  children: ReactNode
  dashboard?: ReactNode
  showDashboard?: boolean
  showReactFlowElements?: boolean
  config: ReactFlowTestingConfig
}

export const ReactFlowTesting: FC<ReactFlowTestingProps> = ({
  children,
  dashboard,
  showDashboard = false,
  showReactFlowElements = false,
  config,
}) => {
  const { reset, onAddNodes, onAddEdges, nodes, edges } = useWorkspaceStore()

  useEffect(() => {
    reset()
  }, [reset])

  useEffect(() => {
    const { initialState } = config
    if (initialState?.nodes)
      onAddNodes(
        initialState.nodes.map((node) => ({
          item: node,
          type: 'add',
        }))
      )
    if (initialState?.edges)
      onAddEdges(
        initialState.edges.map((edge) => ({
          item: edge,
          type: 'add',
        }))
      )

    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  return (
    <EdgeFlowProvider>
      <ReactFlowProvider>
        {/*<SuspenseOutlet />*/}
        {children}
        {dashboard && showDashboard && (
          <Card mt={50} size="sm" variant="filled" colorScheme="red">
            <CardHeader>Testing Dashboard</CardHeader>
            <CardBody>{dashboard}</CardBody>
            {showReactFlowElements && (
              <CardBody>
                <Box>
                  Nodes:
                  {nodes.map((e) => (
                    <Tag colorScheme="green" data-testid="react-flow-nodes" key={e.id}>
                      {e.id}
                    </Tag>
                  ))}
                </Box>
                <Box>
                  Edges:
                  {edges.map((e) => (
                    <Tag colorScheme="blue" data-testid="react-flow-edges" key={e.id}>
                      {e.id}
                    </Tag>
                  ))}
                </Box>
              </CardBody>
            )}
          </Card>
        )}
      </ReactFlowProvider>
    </EdgeFlowProvider>
  )
}
