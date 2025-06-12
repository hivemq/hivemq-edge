import { AccessibleDraggableProvider } from '@/hooks/useAccessibleDraggable'
import type { FC, PropsWithChildren } from 'react'

export const getAccessibleDraggableProvider = () => {
  const WrapperEdgeProvider: FC<PropsWithChildren> = ({ children }) => {
    return <AccessibleDraggableProvider>{children}</AccessibleDraggableProvider>
  }

  return WrapperEdgeProvider
}
