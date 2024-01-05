import { ReactNode } from 'react'
import { EdgeFlowProvider } from '@/modules/Workspace/hooks/FlowContext.tsx'
import { ReactFlowProvider } from 'reactflow'
import { EdgeFlowOptions } from '@/modules/Workspace/types.ts'
import 'reactflow/dist/style.css'

export const mockReactFlow = (children: ReactNode, defaults?: Partial<EdgeFlowOptions>) => (
  <EdgeFlowProvider defaults={defaults}>
    <ReactFlowProvider>
      <div className="react-flow__node selectable">{children}</div>
    </ReactFlowProvider>
  </EdgeFlowProvider>
)
