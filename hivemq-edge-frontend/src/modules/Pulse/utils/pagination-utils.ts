import { AssetMapping } from '@/api/__generated__'

export const MAX_SOURCES_PER_ROW = 4

const STATUS_ERROR = AssetMapping.status.UNMAPPED
const STATUS_ORDER = {
  [AssetMapping.status.UNMAPPED]: 0,
  [AssetMapping.status.DRAFT]: 1,
  [AssetMapping.status.STREAMING]: 2,
  [AssetMapping.status.REQUIRES_REMAPPING]: 3,
  [AssetMapping.status.MISSING]: 4,
} as const

export function compareStatus(rowA: AssetMapping.status | undefined, rowB: AssetMapping.status | undefined) {
  const a = STATUS_ORDER[rowA || STATUS_ERROR]
  const b = STATUS_ORDER[rowB || STATUS_ERROR]

  if (a > b) return 1
  if (a < b) return -1
  return 0
}
