import { type FC, type PropsWithChildren } from 'react'
import { FocusLock, chakra as Chakra, type BoxProps, ModalOverlay } from '@chakra-ui/react'

import config from '@/config'
import { Z_INDICES } from '@/modules/Theme/utils'

import { useAccessibleDraggable } from './useAccessibleDraggable'

export const AccessibleDraggableLock: FC<PropsWithChildren> = ({ children }) => {
  const { isDragging, ref } = useAccessibleDraggable()

  const dragStyle: Partial<BoxProps> = {
    backgroundColor: 'white',
    position: 'relative',
  }

  return (
    <FocusLock isDisabled={!isDragging} finalFocusRef={ref} restoreFocus>
      {isDragging && <ModalOverlay />}
      <Chakra.div
        zIndex={Z_INDICES.ACCESSIBLE_DRAG_N_DROP}
        {...(isDragging && config.isDevMode && dragStyle)}
        sx={{
          '&:focus-visible': {
            boxShadow: 'var(--chakra-shadows-outline)',
            outline: 'unset',
          },
        }}
      >
        {children}
      </Chakra.div>
    </FocusLock>
  )
}
