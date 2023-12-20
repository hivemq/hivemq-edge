import { FC, lazy, Suspense, useEffect } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { Node, useEdges, useNodes } from 'reactflow'
import { AbsoluteCenter, useDisclosure } from '@chakra-ui/react'

import { Adapter, Bridge } from '@/api/__generated__'
import LoaderSpinner from '@/components/Chakra/LoaderSpinner.tsx'
import { AdapterNavigateState, ProtocolAdapterTabIndex } from '@/modules/ProtocolAdapters/types.ts'

import { EdgeTypes, Group, NodeTypes } from '../../types.ts'

const NodePropertyDrawer = lazy(() => import('../drawers/NodePropertyDrawer.tsx'))
const LinkPropertyDrawer = lazy(() => import('../drawers/LinkPropertyDrawer.tsx'))
const GroupPropertyDrawer = lazy(() => import('../drawers/GroupPropertyDrawer.tsx'))

const NodePanelController: FC = () => {
  const navigate = useNavigate()
  const { isOpen, onOpen, onClose } = useDisclosure()

  const nodes = useNodes()
  const edges = useEdges()

  const { nodeId } = useParams()

  const selectedNode = nodes.find(
    (e) => e.id === nodeId && (e.type === NodeTypes.BRIDGE_NODE || e.type === NodeTypes.ADAPTER_NODE)
  ) as Node<Bridge | Adapter> | undefined

  const selectedLinkSource = nodes.find((e) => {
    const link = edges.find((e) => e.id === nodeId && e.type === EdgeTypes.REPORT_EDGE)
    if (!link) return undefined
    return e.id === link.source // && (e.type === NodeTypes.BRIDGE_NODE || e.type === NodeTypes.ADAPTER_NODE)
  }) as Node<Bridge | Adapter | Group> | undefined

  const selectedGroup = nodes.find((e) => e.id === nodeId && e.type === NodeTypes.CLUSTER_NODE) as
    | Node<Group>
    | undefined

  useEffect(() => {
    if (!nodes.length) return
    // if (!selectedNode || !nodeId) {
    //   navigate('/edge-flow', { replace: true })
    //   return
    // }
    onOpen()
  }, [navigate, nodeId, nodes.length, onOpen, selectedNode])

  const handleClose = () => {
    onClose()
    navigate('/edge-flow')
  }

  const handleEditEntity = () => {
    if (selectedNode?.type === NodeTypes.ADAPTER_NODE) {
      const adapterNavigateState: AdapterNavigateState = {
        protocolAdapterTabIndex: ProtocolAdapterTabIndex.adapters,
        protocolAdapterType: (selectedNode?.data as Adapter).type,
        selectedActiveAdapter: { isNew: false, isOpen: false, adapterId: (selectedNode?.data as Adapter).id },
      }
      navigate(`/protocol-adapters/${(selectedNode?.data as Adapter).id}`, {
        state: adapterNavigateState,
      })
    } else if (selectedNode?.type === NodeTypes.BRIDGE_NODE) {
      navigate(`/mqtt-bridges/${(selectedNode?.data as Bridge).id}`)
    }
  }

  if (!nodeId) return null

  return (
    <Suspense
      // TODO[NVL] Would be good to integrate the loader within the drawer
      fallback={
        <AbsoluteCenter axis="both">
          <LoaderSpinner />
        </AbsoluteCenter>
      }
    >
      {selectedLinkSource && selectedLinkSource.type !== NodeTypes.CLUSTER_NODE && (
        <LinkPropertyDrawer
          nodeId={nodeId}
          selectedNode={selectedLinkSource as Node<Bridge | Adapter>}
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
      {selectedNode && (
        <NodePropertyDrawer
          nodeId={nodeId}
          selectedNode={selectedNode}
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
