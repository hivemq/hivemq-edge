import type { XYPosition } from 'reactflow'
import { CANVAS_GRID } from '@datahub/utils/theme.utils.ts'

export const CANVAS_DROP_DELTA: XYPosition = {
  x: -CANVAS_GRID * 8,
  y: -CANVAS_GRID * 2,
}

// The delta positions used to locate nodes on the canvas at loading time, as position is not serialised
export const CANVAS_POSITION = {
  Client: { x: -300, y: 0 } as XYPosition,
  Topic: { x: -300, y: 0 } as XYPosition,
  Function: { x: 350, y: -400 } as XYPosition,
  Transition: { x: 400, y: 100 } as XYPosition,
  PolicySchema: { x: 0, y: -150 } as XYPosition,
  Validator: { x: 0, y: -150 } as XYPosition,
  OperationSuccess: { x: 200, y: 0 } as XYPosition,
  OperationError: { x: 200, y: 100 } as XYPosition,
}
