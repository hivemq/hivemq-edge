import { useContext } from 'react'
import { AccessibleDraggableContext } from './index'

export const useAccessibleDraggable = () => {
  const context = useContext(AccessibleDraggableContext)

  if (context === null) {
    throw Error('useAccessibleDraggable must be used within AccessibleDraggableContext')
  }
  return context
}
