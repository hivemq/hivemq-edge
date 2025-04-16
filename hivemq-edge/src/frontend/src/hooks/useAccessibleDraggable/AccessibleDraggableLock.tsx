import { FocusLock } from '@chakra-ui/react'
import { type FC, type PropsWithChildren } from 'react'
import { useAccessibleDraggable } from './useAccessibleDraggable'

export const AccessibleDraggableLock: FC<PropsWithChildren> = ({ children }) => {
  const { isDragging } = useAccessibleDraggable()
  return <FocusLock isDisabled={!isDragging}>{children}</FocusLock>
}
