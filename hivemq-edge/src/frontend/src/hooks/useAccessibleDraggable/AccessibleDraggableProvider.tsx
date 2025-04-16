import { type FC, type PropsWithChildren, useCallback, useEffect, useState } from 'react'
import { useToast } from '@chakra-ui/react'
import { useHotkeys } from 'react-hotkeys-hook'

import { DATAHUB_HOTKEY } from '@/extensions/datahub/utils/datahub.utils'
import { AccessibleDraggableContext, type AccessibleDraggableProps } from './index'

const TOAST_DRAGGABLE = 'TOAST_DRAGGABLE'

export const AccessibleDraggableProvider: FC<PropsWithChildren> = ({ children }) => {
  const [isDragging, setIsDragging] = useState(false)
  const toast = useToast({ id: TOAST_DRAGGABLE })

  const onStartDragging = useCallback(() => {
    setIsDragging(true)
    if (!toast.isActive(TOAST_DRAGGABLE)) toast({ duration: null, isClosable: true, description: `Dragging is active` })
  }, [toast])

  const onEndDragging = useCallback(() => {
    setIsDragging(false)
    toast.close(TOAST_DRAGGABLE)
  }, [toast])

  // TODO[NVL] ESCAPE is caught by Chakra (for closing the modals) and cannot be intercepted yet; reverting to BACKSPACE
  useHotkeys([DATAHUB_HOTKEY.ESCAPE, DATAHUB_HOTKEY.BACKSPACE], () => {
    onEndDragging()
  })

  useEffect(() => {
    const handleMouseDown = () => {
      onEndDragging()
    }

    document.addEventListener('mousedown', handleMouseDown)
    return () => {
      document.removeEventListener('mousedown', handleMouseDown)
    }
  }, [onEndDragging])

  const defaultAccessibleDraggableValues: AccessibleDraggableProps = {
    isDragging: isDragging,
    startDragging: onStartDragging,
    endDragging: onEndDragging,
  }

  return (
    <AccessibleDraggableContext.Provider value={defaultAccessibleDraggableValues}>
      {children}
    </AccessibleDraggableContext.Provider>
  )
}
