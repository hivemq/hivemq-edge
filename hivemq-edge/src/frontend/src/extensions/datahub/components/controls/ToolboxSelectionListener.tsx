import type { FC } from 'react'
import { useOnSelectionChange } from 'reactflow'

import { usePolicyChecksStore } from '@datahub/hooks/usePolicyChecksStore.ts'
import { isBehaviorPolicyNodeType, isDataPolicyNodeType } from '@datahub/utils/node.utils.ts'

const ToolboxSelectionListener: FC = () => {
  const { setNode } = usePolicyChecksStore()

  useOnSelectionChange({
    onChange: ({ nodes }) => {
      if (nodes.length === 1) {
        const [node] = nodes
        if (isDataPolicyNodeType(node) || isBehaviorPolicyNodeType(node)) {
          setNode(node)
          return
        }
      }
      setNode(undefined)
    },
  })

  return <div></div>
}

export default ToolboxSelectionListener
