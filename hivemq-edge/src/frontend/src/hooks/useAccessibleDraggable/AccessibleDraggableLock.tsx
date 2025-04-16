import { type FC, type PropsWithChildren } from 'react'
import { FocusLock, chakra as Chakra, type BoxProps } from '@chakra-ui/react'

import config from '@/config'

import { useAccessibleDraggable } from './useAccessibleDraggable'

export const AccessibleDraggableLock: FC<PropsWithChildren> = ({ children }) => {
  const { isDragging } = useAccessibleDraggable()

  const devStyle: Partial<BoxProps> = {
    backgroundColor: 'red',
    borderColor: 'red',
    borderWidth: 2,
  }
  return (
    <FocusLock isDisabled={!isDragging}>
      <Chakra.div {...(isDragging && config.isDevMode && devStyle)}>{children}</Chakra.div>
    </FocusLock>
  )
}
