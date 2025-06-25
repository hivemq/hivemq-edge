import type { DataHubNodeType } from '@datahub/types.ts'

export const vitest_ExpectStringContainingUUIDFromNodeType = (type: DataHubNodeType) => {
  return expect.stringContaining(`${type}_`)
}
