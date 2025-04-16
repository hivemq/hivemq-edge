import type { DataReference } from '@/api/hooks/useDomainModel/useGetCombinedDataSchemas'
import type { FlatJSONSchema7 } from '@/components/rjsf/MqttTransformation/utils/json-schema.utils'

export interface AccessibleDraggableProps {
  isDragging: boolean
  startDragging: (data: { property: FlatJSONSchema7; dataReference?: DataReference | undefined }) => void
  endDragging: () => void
}

export const EDGE_HOTKEY = {
  ENTER: 'Enter',
  BACKSPACE: 'Backspace',
  DELETE: 'Delete',
  ESC: 'ESC',
  ESCAPE: 'Escape',
}
