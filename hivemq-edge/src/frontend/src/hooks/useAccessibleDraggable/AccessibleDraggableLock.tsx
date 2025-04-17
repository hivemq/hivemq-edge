import { type FC, type PropsWithChildren } from 'react'
import { FocusLock, chakra as Chakra, type BoxProps, ModalOverlay } from '@chakra-ui/react'

import config from '@/config'

import { useAccessibleDraggable } from './useAccessibleDraggable'

export const AccessibleDraggableLock: FC<PropsWithChildren> = ({ children }) => {
  const { isDragging } = useAccessibleDraggable()

  const dragStyle: Partial<BoxProps> = {
    backgroundColor: 'white',
    position: 'relative',
  }

  return (
    <FocusLock isDisabled={!isDragging}>
      {isDragging && <ModalOverlay />}
      <Chakra.div
        zIndex={1800}
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
