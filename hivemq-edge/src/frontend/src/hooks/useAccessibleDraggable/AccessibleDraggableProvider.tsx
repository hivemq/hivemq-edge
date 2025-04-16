import { type FC, type PropsWithChildren, useState } from 'react'
import { AccessibleDraggableContext, type AccessibleDraggableProps } from './index'

export const AccessibleDraggableProvider: FC<PropsWithChildren> = ({ children }) => {
  const [isDragging, setIsDragging] = useState(false)

  const defaultAccessibleDraggableValues: AccessibleDraggableProps = {
    isDragging: isDragging,
    startDragging: () => setIsDragging(true),
    endDragging: () => setIsDragging(false),
  }

  return (
    <AccessibleDraggableContext.Provider value={defaultAccessibleDraggableValues}>
      {children}
    </AccessibleDraggableContext.Provider>
  )
}
