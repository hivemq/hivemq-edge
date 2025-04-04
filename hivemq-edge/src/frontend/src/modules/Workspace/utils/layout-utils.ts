import type { Node } from '@xyflow/react'

import type { Adapter, Bridge } from '@/api/__generated__'
import type { EdgeFlowGrouping } from '@/modules/Workspace/types.ts'
import { EdgeFlowLayout } from '@/modules/Workspace/types.ts'

export const applyLayout = (nodes: Node<Bridge | Adapter>[], groupOption: EdgeFlowGrouping): Node[] => {
  // TODO Implements better layouts for the workspace
  switch (groupOption.layout) {
    case EdgeFlowLayout.HORIZONTAL:
    default:
      return nodes
  }
}
