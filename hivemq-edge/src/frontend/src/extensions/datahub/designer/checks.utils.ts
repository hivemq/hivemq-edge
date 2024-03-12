import { XYPosition } from 'reactflow'

// The delta positions used to locate nodes on the canvas at loading time, as position is not serialised
export const CANVAS_POSITION = {
  Client: { x: -300, y: 0 } as XYPosition,
  Topic: { x: -300, y: 0 } as XYPosition,
  Function: { x: 0, y: -275 } as XYPosition,
  Transition: { x: 350, y: -100 } as XYPosition,
  Schema: { x: 0, y: -150 } as XYPosition,
  Validator: { x: 0, y: -150 } as XYPosition,
  OperationSuccess: { x: 400, y: 0 } as XYPosition,
  OperationError: { x: 400, y: 100 } as XYPosition,
}
