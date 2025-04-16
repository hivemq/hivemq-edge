import type { DataReference } from '@/api/hooks/useDomainModel/useGetCombinedDataSchemas'
import type { FlatJSONSchema7 } from '@/components/rjsf/MqttTransformation/utils/json-schema.utils'

export interface AccessibleDraggableProps {
  isDragging: boolean
  isValidDrop: (target: FlatJSONSchema7) => boolean
  startDragging: (data: { property: FlatJSONSchema7; dataReference?: DataReference | undefined }) => void
  endDragging: (property?: FlatJSONSchema7) => void
  source?: {
    property: FlatJSONSchema7
    dataReference?: DataReference | undefined
  }
}

export const EDGE_HOTKEY = {
  ENTER: 'Enter',
  BACKSPACE: 'Backspace',
  DELETE: 'Delete',
  ESC: 'ESC',
  ESCAPE: 'Escape',
}
