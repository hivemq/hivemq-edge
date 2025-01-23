import type { FC, PropsWithChildren } from 'react'
import { EdgeFlowProvider } from '@/modules/Workspace/hooks/FlowContext.tsx'
import type { EdgeFlowOptions } from '@/modules/Workspace/types.ts'
import { SimpleWrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'

export const getWrapperEdgeProvider = (defaults?: Partial<EdgeFlowOptions>) => {
  const WrapperEdgeProvider: FC<PropsWithChildren> = ({ children }) => {
    return (
      <SimpleWrapper>
        <EdgeFlowProvider defaults={defaults}>{children}</EdgeFlowProvider>
      </SimpleWrapper>
    )
  }

  return WrapperEdgeProvider
}
