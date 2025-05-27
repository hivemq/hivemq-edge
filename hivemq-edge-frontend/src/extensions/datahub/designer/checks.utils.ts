import type { XYPosition } from '@xyflow/react'
import { CANVAS_GRID } from '@datahub/utils/theme.utils.ts'

export const CANVAS_DROP_DELTA: XYPosition = {
  x: -CANVAS_GRID * 8,
  y: -CANVAS_GRID * 2,
}

// The delta positions used to locate nodes on the canvas at loading time, as position is not serialised
export const CANVAS_POSITION = {
  Client: { x: -10 * CANVAS_GRID, y: 0 } as XYPosition,
  Topic: { x: -10 * CANVAS_GRID, y: 0 } as XYPosition,
  Function: { x: -10 * CANVAS_GRID, y: CANVAS_GRID * 5 } as XYPosition,
  Transition: { x: 10 * CANVAS_GRID, y: 4 * CANVAS_GRID } as XYPosition,
  PolicySchema: { x: -10 * CANVAS_GRID, y: CANVAS_GRID * 10 } as XYPosition,
  SchemaOperation: { x: -10 * CANVAS_GRID, y: CANVAS_GRID * 5 } as XYPosition,
  Validator: { x: -10 * CANVAS_GRID, y: CANVAS_GRID * 5 } as XYPosition,
  OperationSuccess: { x: 10 * CANVAS_GRID, y: 0 } as XYPosition,
  OperationError: { x: 10 * CANVAS_GRID, y: CANVAS_GRID * 4 } as XYPosition,
}
