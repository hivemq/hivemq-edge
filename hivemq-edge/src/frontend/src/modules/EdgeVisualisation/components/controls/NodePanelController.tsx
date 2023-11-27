import { FC, useEffect } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { Node, useEdges, useNodes } from 'reactflow'
import { useDisclosure } from '@chakra-ui/react'

import { Adapter, Bridge } from '@/api/__generated__'
import { AdapterNavigateState, ProtocolAdapterTabIndex } from '@/modules/ProtocolAdapters/types.ts'

import { EdgeTypes, NodeTypes } from '../../types.ts'
import NodePropertyDrawer from '../drawers/NodePropertyDrawer.tsx'
import LinkPropertyDrawer from '../drawers/LinkPropertyDrawer.tsx'

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
    return e.id === link.source && (e.type === NodeTypes.BRIDGE_NODE || e.type === NodeTypes.ADAPTER_NODE)
  }) as Node<Bridge | Adapter> | undefined

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

  if (selectedLinkSource)
    return (
      <LinkPropertyDrawer
        selectedNode={selectedLinkSource}
        isOpen={isOpen}
        onClose={handleClose}
        onEditEntity={handleEditEntity}
      />
    )

  if (!selectedNode || !selectedNode.type) {
    return null
  }

  return (
    <NodePropertyDrawer
      selectedNode={selectedNode}
      isOpen={isOpen}
      onClose={handleClose}
      onEditEntity={handleEditEntity}
    />
  )
}

export default NodePanelController
