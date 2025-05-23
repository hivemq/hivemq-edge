import { expect, expectTypeOf } from 'vitest'

import { groupCatalog } from './cluster.utils'

describe('groupCatalog', () => {
  it('should return a valid catalog of groups', async () => {
    expect(groupCatalog).toHaveLength(4)

    expect(groupCatalog.find((e) => e.key === 'runtime')).toStrictEqual(
      expect.objectContaining({
        key: 'runtime',
        name: 'Runtime Status',
      })
    )

    expect(groupCatalog.find((e) => e.key === 'startedAt')).toStrictEqual(
      expect.objectContaining({
        key: 'startedAt',
        name: 'Started at',
      })
    )

    groupCatalog.forEach((group) => {
      expectTypeOf(group.keyFunction).toBeFunction
    })
  })

  it.skip('should return valid functions', async () => {
    // TODO[TEST] Need to test the validity of the grouping key on data structures
  })
})
