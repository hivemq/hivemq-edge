import type { FC, PropsWithChildren } from 'react'

import { SimpleWrapper } from '@/__test-utils__/hooks/SimpleWrapper.tsx'
import type { EdgeFlowOptions } from '@/modules/Workspace/types.ts'
import { EdgeFlowProvider } from '@/modules/Workspace/hooks/EdgeFlowProvider.tsx'

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
