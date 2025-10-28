import type { DataHubNodeType } from '@datahub/types.ts'
import { expect } from 'vitest'

export const vitest_ExpectStringContainingUUIDFromNodeType = (type: DataHubNodeType) => {
  return expect.stringContaining(`${type}_`)
}
