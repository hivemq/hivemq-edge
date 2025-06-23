import type { HotKeyItem } from '@datahub/types.ts'
import { OperationData } from '@datahub/types.ts'

export const DND_DESIGNER_NODE_TYPE = 'application/reactflow;type=designer-node'

export const SCRIPT_FUNCTION_SEPARATOR = ':'
export const SCRIPT_FUNCTION_PREFIX = 'fn'
export const SCRIPT_FUNCTION_LATEST = 'latest'

export const DRYRUN_VALIDATION_DELAY = 150

export const DATAHUB_HOTKEY = {
  COPY: 'Meta+C',
  PASTE: 'Meta+V',
  ESCAPE: 'ESC',
  BACKSPACE: 'Backspace',
  DELETE: 'Delete',
}

export const DATAHUB_HOTKEY_CONTEXT: HotKeyItem[] = [
  { key: 'TAB', category: 'Designer' },
  { key: 'ENTER', category: 'Designer' },
  { key: 'Meta+ENTER', category: 'Designer' },
  { key: DATAHUB_HOTKEY.PASTE, category: 'Designer' },
  { key: DATAHUB_HOTKEY.ESCAPE, category: 'Designer' },
  { key: DATAHUB_HOTKEY.COPY, category: 'Selected' },
  { key: 'ArrowLeft', category: 'Selected' },
  { key: 'ArrowRight', category: 'Selected' },
  { key: 'ArrowUp', category: 'Selected' },
  { key: 'ArrowDown', category: 'Selected' },
  { key: 'Backspace', category: 'Selected' },
  { key: 'Shift+drag', category: 'Designer' },
  { key: 'Meta+click', category: 'Designer' },
]

/**
 * List of operation functions that are not allowed to be used in the DataHub Designer.
 */
export const OPERATION_FUNCTION_BLACKLIST: string[] = [
  OperationData.Function.SERDES_DESERIALIZE,
  OperationData.Function.SERDES_SERIALIZE,
]
