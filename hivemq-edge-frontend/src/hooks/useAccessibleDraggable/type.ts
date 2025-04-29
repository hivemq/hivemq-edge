import type { MutableRefObject, RefObject } from 'react'
import type { FocusableElement } from '@chakra-ui/utils'

import type { DataReference } from '@/api/hooks/useDomainModel/useGetCombinedDataSchemas'
import type { FlatJSONSchema7 } from '@/components/rjsf/MqttTransformation/utils/json-schema.utils'

export interface AccessibleDraggableProps {
  isDragging: boolean
  isValidDrop: (target: FlatJSONSchema7) => boolean
  startDragging: (data: {
    property: FlatJSONSchema7
    dataReference?: DataReference | undefined
    ref?: MutableRefObject<HTMLDivElement | null>
  }) => void
  endDragging: (property?: FlatJSONSchema7) => void
  source?: {
    property: FlatJSONSchema7
    dataReference?: DataReference | undefined
  }
  ref?: RefObject<FocusableElement>
}

export const EDGE_HOTKEY = {
  ENTER: 'Enter',
  BACKSPACE: 'Backspace',
  DELETE: 'Delete',
  ESC: 'ESC',
  ESCAPE: 'Escape',
}
