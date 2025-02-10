import type { ReactNode } from 'react'
import { ReactFlowProvider } from 'reactflow'
import type { EdgeFlowOptions } from '@/modules/Workspace/types.ts'
import { EdgeFlowProvider } from '@/modules/Workspace/hooks/EdgeFlowProvider.tsx'

import 'reactflow/dist/style.css'

export const mockReactFlow = (children: ReactNode, defaults?: Partial<EdgeFlowOptions>) => (
  <EdgeFlowProvider defaults={defaults}>
    <ReactFlowProvider>
      <div className="react-flow__node selectable">{children}</div>
    </ReactFlowProvider>
  </EdgeFlowProvider>
)
