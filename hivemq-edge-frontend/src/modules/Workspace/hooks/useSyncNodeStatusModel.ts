import { useEffect } from 'react'
import { useReactFlow } from '@xyflow/react'

import type { NodeStatusModel } from '@/modules/Workspace/types/status.types'

/**
 * Compares the parts of a status model that drive rendering.
 *
 * `originalStatus` and `lastUpdated` are carried for traceability only, and `lastUpdated` defaults
 * to the time the model was built, so comparing them would report a change on every recomputation.
 */
export const isSameStatusModel = (current?: NodeStatusModel, next?: NodeStatusModel) =>
  current?.runtime === next?.runtime && current?.operational === next?.operational && current?.source === next?.source

/**
 * Stores a node's computed status model on its data, skipping the write when nothing changed.
 *
 * Nodes that derive their status from their connections observe their own data doing so: a node's
 * outbound connections list the node itself as the source, so it ends up in the `useNodesData`
 * selection. The status model is rebuilt on every one of those changes, and writing an equal but
 * newly allocated object back would feed the next render, which React 19 reports as
 * "Maximum update depth exceeded" and the workspace error boundary turns into a blank canvas.
 *
 * @param id - The node to update
 * @param statusModel - The status model computed from the node's own state and its connections
 * @param currentStatusModel - The status model currently stored on the node's data
 */
export const useSyncNodeStatusModel = (
  id: string,
  statusModel: NodeStatusModel,
  currentStatusModel?: NodeStatusModel
) => {
  const { updateNodeData } = useReactFlow()

  useEffect(() => {
    if (isSameStatusModel(currentStatusModel, statusModel)) return
    updateNodeData(id, { statusModel })
  }, [id, statusModel, currentStatusModel, updateNodeData])
}
