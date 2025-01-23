import type { FC, ReactElement } from 'react'
import { useState } from 'react'
import type { Edge, EdgeAddChange, Node, NodeAddChange, NodeSelectionChange, XYPosition } from 'reactflow'
import { getConnectedEdges } from 'reactflow'
import { v4 as uuidv4 } from 'uuid'
import { useHotkeys } from 'react-hotkeys-hook'

import useDataHubDraftStore from '@datahub/hooks/useDataHubDraftStore.ts'
import { DATAHUB_HOTKEY } from '@datahub/utils/datahub.utils.ts'
import { usePolicyGuards } from '@datahub/hooks/usePolicyGuards.ts'

const DEFAULT_POSITION_DELTA: XYPosition = { x: 100, y: 75 }

interface CopyPasteListenerProps {
  render?: (nodes: Node[]) => ReactElement
}

export const CopyPasteListener: FC<CopyPasteListenerProps> = ({ render }) => {
  const { nodes, edges, onNodesChange, onEdgesChange } = useDataHubDraftStore()
  const [copiedNodes, setCopiedNodes] = useState<Node[]>([])
  const [delta, setDelta] = useState<XYPosition>(DEFAULT_POSITION_DELTA)
  const { isPolicyEditable } = usePolicyGuards()

  useHotkeys(DATAHUB_HOTKEY.ESCAPE, () => {
    setCopiedNodes([])
    setDelta(DEFAULT_POSITION_DELTA)
    onNodesChange(nodes.map<NodeSelectionChange>((node) => ({ id: node.id, type: 'select', selected: false })))
  })

  useHotkeys(DATAHUB_HOTKEY.COPY, () => {
    if (!copiedNodes.length && !isPolicyEditable) return

    const selectedNodes = nodes.filter((node) => node.selected)
    if (selectedNodes.length) {
      setCopiedNodes(selectedNodes)
    } else setCopiedNodes([])
    setDelta(DEFAULT_POSITION_DELTA)
  })

  useHotkeys(DATAHUB_HOTKEY.PASTE, () => {
    if (!copiedNodes.length && !isPolicyEditable) return

    const ids = copiedNodes.map((node) => node.id)
    const newIds = copiedNodes.reduce<Record<string, string>>((acc, node) => {
      acc[node.id] = uuidv4()
      return acc
    }, {})

    const selectedEdges = getConnectedEdges(copiedNodes, edges).filter(
      (edge) => ids.includes(edge.source) && ids.includes(edge.target)
    )

    const duplicateNodes = copiedNodes.map<Node>((node) => ({
      ...node,
      id: newIds[node.id],
      position: { x: node.position.x + delta.x, y: node.position.y + delta.y },
      selected: true,
    }))
    const duplicateEdges = selectedEdges.map<Edge>((edge) => ({
      ...edge,
      id: uuidv4(),
      source: newIds[edge.source],
      target: newIds[edge.target],
    }))
    setDelta({ x: delta.x + DEFAULT_POSITION_DELTA.x, y: delta.y + DEFAULT_POSITION_DELTA.y })

    onNodesChange([
      ...copiedNodes.map<NodeSelectionChange>((node) => ({ id: node.id, type: 'select', selected: false })),
      ...duplicateNodes.map<NodeAddChange>((node) => ({ item: node, type: 'add' })),
    ])
    onEdgesChange([...duplicateEdges.map<EdgeAddChange>((edge) => ({ item: edge, type: 'add' }))])
  })

  return render?.(copiedNodes) || null
}
