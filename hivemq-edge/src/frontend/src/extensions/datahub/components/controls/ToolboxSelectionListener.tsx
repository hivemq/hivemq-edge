import type { FC } from 'react'
import { useCallback } from 'react'
import type { Edge, Node } from '@xyflow/react'
import { useOnSelectionChange } from '@xyflow/react'

import { usePolicyChecksStore } from '@datahub/hooks/usePolicyChecksStore.ts'
import { isBehaviorPolicyNodeType, isDataPolicyNodeType } from '@datahub/utils/node.utils.ts'

const ToolboxSelectionListener: FC = () => {
  const { setNode } = usePolicyChecksStore()

  const onChange = useCallback<(params: { nodes: Node[]; edges: Edge[] }) => void>(
    ({ nodes }) => {
      if (nodes.length === 1) {
        const [node] = nodes
        if (isDataPolicyNodeType(node) || isBehaviorPolicyNodeType(node)) {
          setNode(node)
          return
        }
      }
      setNode(undefined)
    },
    [setNode]
  )

  useOnSelectionChange({ onChange })

  return null
}

export default ToolboxSelectionListener
