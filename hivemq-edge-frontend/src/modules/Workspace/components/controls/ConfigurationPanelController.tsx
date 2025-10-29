import type { Adapter, Bridge } from '@/api/__generated__'
import { SuspenseFallback } from '@/components/SuspenseOutlet.tsx'
import type { AdapterNavigateState } from '@/modules/ProtocolAdapters/types.ts'
import { ProtocolAdapterTabIndex } from '@/modules/ProtocolAdapters/types.ts'
import type {
  NodeAdapterType,
  NodeBridgeType,
  NodeCombinerType,
  NodeDeviceType,
  NodeEdgeType,
  NodeGroupType,
  NodePulseType,
} from '@/modules/Workspace/types.ts'
import { EdgeTypes, NodeTypes } from '@/modules/Workspace/types.ts'
import { useDisclosure } from '@chakra-ui/react'
import { useEdges, useNodes } from '@xyflow/react'
import type { FC } from 'react'
import { lazy, Suspense, useEffect, useMemo } from 'react'
import { useNavigate, useOutlet, useParams } from 'react-router-dom'

const DevicePropertyDrawer = lazy(() => import('../drawers/DevicePropertyDrawer.tsx'))
const NodePropertyDrawer = lazy(() => import('../drawers/NodePropertyDrawer.tsx'))
const LinkPropertyDrawer = lazy(() => import('../drawers/LinkPropertyDrawer.tsx'))
const GroupPropertyDrawer = lazy(() => import('../drawers/GroupPropertyDrawer.tsx'))
const EdgePropertyDrawer = lazy(() => import('../drawers/EdgePropertyDrawer.tsx'))
const PulsePropertyDrawer = lazy(() => import('../drawers/PulsePropertyDrawer.tsx'))

interface ConfigurationPanelControllerProps {
  type: NodeTypes | EdgeTypes
}

const ConfigurationPanelController: FC<ConfigurationPanelControllerProps> = ({ type }) => {
  const navigate = useNavigate()
  const { isOpen, onOpen, onClose } = useDisclosure()
  const outletElement = useOutlet()
  const nodes = useNodes()
  const edges = useEdges()

  const { adapterId, bridgeId, groupId, deviceId, connectorId } = useParams()

  const selectedNode = useMemo(() => {
    switch (type) {
      case NodeTypes.EDGE_NODE:
        return nodes.find((node) => node.type === type) as NodeEdgeType | undefined
      case NodeTypes.PULSE_NODE:
        return nodes.find((node) => node.type === type) as NodePulseType | undefined
      case NodeTypes.DEVICE_NODE:
        return nodes.find((node) => node.id === deviceId && node.type === type) as NodeDeviceType | undefined
      case NodeTypes.CLUSTER_NODE:
        return nodes.find((node) => node.id === groupId && node.type === type) as NodeGroupType | undefined
      case NodeTypes.BRIDGE_NODE:
        return nodes.find((node) => node.id === bridgeId && node.type === type) as NodeBridgeType | undefined
      case NodeTypes.ADAPTER_NODE:
        return nodes.find((node) => node.id === adapterId && node.type === type) as NodeAdapterType | undefined
    }

    return undefined
  }, [adapterId, bridgeId, deviceId, groupId, nodes, type])

  const selectedEdge = useMemo(() => {
    switch (type) {
      case EdgeTypes.DYNAMIC_EDGE:
        return nodes.find((node) => {
          const link = edges.find((edge) => edge.id === connectorId && edge.type === type)
          if (!link) return undefined
          return node.id === link.source
        }) as NodeBridgeType | NodeAdapterType | NodeGroupType | NodeCombinerType | undefined
    }

    return undefined
  }, [connectorId, edges, nodes, type])

  useEffect(() => {
    if (!nodes.length || !selectedNode) return
    onOpen()
  }, [nodes.length, onOpen, selectedNode])

  useEffect(() => {
    if (!nodes.length || !selectedEdge) return
    onOpen()
  }, [nodes.length, onOpen, selectedEdge])

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

  if (!selectedNode && !selectedEdge) throw new Error('No node or edge selected')

  // If the outlet element is defined, return it. This allows the configuration panel to be rendered in a nested route.
  if (outletElement) return outletElement

  return (
    <Suspense fallback={<SuspenseFallback />}>
      {selectedEdge && selectedEdge?.type !== NodeTypes.CLUSTER_NODE && (
        <LinkPropertyDrawer
          nodeId={selectedEdge.type}
          selectedNode={selectedEdge}
          isOpen={isOpen}
          onClose={handleClose}
          onEditEntity={handleEditEntity}
        />
      )}
      {selectedEdge?.type === NodeTypes.CLUSTER_NODE && (
        <GroupPropertyDrawer
          nodeId={selectedEdge.id}
          nodes={nodes}
          selectedNode={selectedEdge}
          isOpen={isOpen}
          onClose={handleClose}
          onEditEntity={handleEditEntity}
        />
      )}
      {selectedNode?.type === NodeTypes.EDGE_NODE && (
        <EdgePropertyDrawer
          nodeId={selectedNode.id}
          selectedNode={selectedNode}
          isOpen={isOpen}
          onClose={handleClose}
          onEditEntity={handleEditEntity}
        />
      )}
      {(selectedNode?.type === NodeTypes.ADAPTER_NODE || selectedNode?.type === NodeTypes.BRIDGE_NODE) && (
        <NodePropertyDrawer
          nodeId={selectedNode.id}
          selectedNode={selectedNode}
          isOpen={isOpen}
          onClose={handleClose}
          onEditEntity={handleEditEntity}
        />
      )}
      {selectedNode?.type === NodeTypes.DEVICE_NODE && (
        <DevicePropertyDrawer
          nodeId={selectedNode.id}
          selectedNode={selectedNode}
          isOpen={isOpen}
          onClose={handleClose}
          onEditEntity={handleEditEntity}
        />
      )}
      {selectedNode?.type === NodeTypes.CLUSTER_NODE && (
        <GroupPropertyDrawer
          showConfig
          nodeId={selectedNode.id}
          nodes={nodes}
          selectedNode={selectedNode}
          isOpen={isOpen}
          onClose={handleClose}
          onEditEntity={handleEditEntity}
        />
      )}
      {selectedNode?.type === NodeTypes.PULSE_NODE && (
        <PulsePropertyDrawer
          nodeId={selectedNode.id}
          selectedNode={selectedNode}
          isOpen={isOpen}
          onClose={handleClose}
        />
      )}
    </Suspense>
  )
}

export default ConfigurationPanelController
