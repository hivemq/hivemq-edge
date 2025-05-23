import type { FC } from 'react'
import { lazy, Suspense, useEffect } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import type { Node } from '@xyflow/react'
import { useEdges, useNodes } from '@xyflow/react'
import { useDisclosure } from '@chakra-ui/react'

import type { Adapter, Bridge, Combiner } from '@/api/__generated__'
import { SuspenseFallback } from '@/components/SuspenseOutlet.tsx'
import type { AdapterNavigateState } from '@/modules/ProtocolAdapters/types.ts'
import { ProtocolAdapterTabIndex } from '@/modules/ProtocolAdapters/types.ts'
import type { DeviceMetadata, Group } from '@/modules/Workspace/types.ts'
import { EdgeTypes, NodeTypes } from '@/modules/Workspace/types.ts'

const DevicePropertyDrawer = lazy(() => import('../drawers/DevicePropertyDrawer.tsx'))
const NodePropertyDrawer = lazy(() => import('../drawers/NodePropertyDrawer.tsx'))
const LinkPropertyDrawer = lazy(() => import('../drawers/LinkPropertyDrawer.tsx'))
const GroupPropertyDrawer = lazy(() => import('../drawers/GroupPropertyDrawer.tsx'))
const EdgePropertyDrawer = lazy(() => import('../drawers/EdgePropertyDrawer.tsx'))

const NodePanelController: FC = () => {
  const navigate = useNavigate()
  const { isOpen, onOpen, onClose } = useDisclosure()

  const nodes = useNodes()
  const edges = useEdges()

  const { nodeId } = useParams()

  const selectedNode = nodes.find(
    (node) => node.id === nodeId && (node.type === NodeTypes.BRIDGE_NODE || node.type === NodeTypes.ADAPTER_NODE)
  ) as Node<Bridge | Adapter> | undefined

  const selectedEdge = nodes.find((node) => node.id === nodeId && node.type === NodeTypes.EDGE_NODE)
  const selectedDevice = nodes.find((node) => node.id === nodeId && node.type === NodeTypes.DEVICE_NODE) as
    | Node<DeviceMetadata>
    | undefined

  const selectedLinkSource = nodes.find((node) => {
    const link = edges.find((edge) => edge.id === nodeId && edge.type === EdgeTypes.DYNAMIC_EDGE)
    if (!link) return undefined
    return node.id === link.source
  }) as Node<Bridge | Adapter | Group | Combiner> | undefined

  const selectedGroup = nodes.find((node) => node.id === nodeId && node.type === NodeTypes.CLUSTER_NODE) as
    | Node<Group>
    | undefined

  useEffect(() => {
    if (!nodes.length) return
    onOpen()
  }, [navigate, nodeId, nodes.length, onOpen, selectedNode])

  const handleClose = () => {
    onClose()
    navigate('/workspace')
  }

  const handleEditEntity = () => {
    if (selectedNode?.type === NodeTypes.ADAPTER_NODE) {
      const adapterNavigateState: AdapterNavigateState = {
        protocolAdapterTabIndex: ProtocolAdapterTabIndex.ADAPTERS,
        protocolAdapterType: (selectedNode?.data as Adapter).type,
        selectedActiveAdapter: { isNew: false, isOpen: false, adapterId: (selectedNode?.data as Adapter).id },
      }
      const { id, type } = selectedNode?.data as Adapter
      navigate(`/protocol-adapters/edit/${type}/${id}`, {
        state: adapterNavigateState,
      })
    } else if (selectedNode?.type === NodeTypes.BRIDGE_NODE) {
      const { id } = selectedNode?.data as Bridge
      navigate(`/mqtt-bridges/${id}`)
    }
  }

  if (!nodeId) return null

  return (
    <Suspense fallback={<SuspenseFallback />}>
      {selectedLinkSource && selectedLinkSource.type !== NodeTypes.CLUSTER_NODE && (
        <LinkPropertyDrawer
          nodeId={nodeId}
          selectedNode={selectedLinkSource as Node<Adapter | Bridge | Combiner>}
          isOpen={isOpen}
          onClose={handleClose}
          onEditEntity={handleEditEntity}
        />
      )}
      {selectedLinkSource && selectedLinkSource.type === NodeTypes.CLUSTER_NODE && (
        <GroupPropertyDrawer
          nodeId={nodeId}
          nodes={nodes}
          selectedNode={selectedLinkSource as Node<Group>}
          isOpen={isOpen}
          onClose={handleClose}
          onEditEntity={handleEditEntity}
        />
      )}
      {selectedEdge && (
        <EdgePropertyDrawer
          nodeId={nodeId}
          selectedNode={selectedEdge}
          isOpen={isOpen}
          onClose={handleClose}
          onEditEntity={handleEditEntity}
        />
      )}
      {selectedNode && (
        <NodePropertyDrawer
          nodeId={nodeId}
          selectedNode={selectedNode}
          isOpen={isOpen}
          onClose={handleClose}
          onEditEntity={handleEditEntity}
        />
      )}
      {selectedDevice && (
        <DevicePropertyDrawer
          nodeId={nodeId}
          selectedNode={selectedDevice}
          isOpen={isOpen}
          onClose={handleClose}
          onEditEntity={handleEditEntity}
        />
      )}
      {selectedGroup && (
        <GroupPropertyDrawer
          showConfig
          nodeId={nodeId}
          nodes={nodes}
          selectedNode={selectedGroup}
          isOpen={isOpen}
          onClose={handleClose}
          onEditEntity={handleEditEntity}
        />
      )}
    </Suspense>
  )
}

export default NodePanelController
