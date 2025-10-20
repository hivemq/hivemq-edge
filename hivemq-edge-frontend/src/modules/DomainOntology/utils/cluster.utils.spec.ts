import { expect, expectTypeOf } from 'vitest'
import { DateTime } from 'luxon'
import { Status } from '@/api/__generated__'

import { groupCatalog, TreeEntity } from './cluster.utils'
import type { ClusterDataWrapper } from './cluster.utils'

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

  describe('keyFunction implementations', () => {
    it('should extract runtime status from payload', () => {
      const runtimeGroup = groupCatalog.find((e) => e.key === 'runtime')
      const testData: ClusterDataWrapper = {
        category: TreeEntity.ADAPTER,
        name: 'test-adapter',
        payload: {
          id: 'test',
          type: 'opcua',
          status: {
            runtime: Status.runtime.STARTED,
            connection: Status.connection.CONNECTED,
          },
        },
      }

      expect(runtimeGroup?.keyFunction(testData)).toBe('STARTED')
    })

    it('should extract and format startedAt relative time', () => {
      const startedAtGroup = groupCatalog.find((e) => e.key === 'startedAt')
      const now = DateTime.now().toISO() as string
      const testData: ClusterDataWrapper = {
        category: TreeEntity.ADAPTER,
        name: 'test-adapter',
        payload: {
          id: 'test',
          type: 'opcua',
          status: {
            startedAt: now,
          },
        },
      }

      const result = startedAtGroup?.keyFunction(testData)
      expect(result).toBeDefined()
      expect(typeof result).toBe('string')
    })

    it('should extract adapter type from payload', () => {
      const typeGroup = groupCatalog.find((e) => e.key === 'type')
      const testData: ClusterDataWrapper = {
        category: TreeEntity.ADAPTER,
        name: 'test-adapter',
        payload: {
          id: 'test',
          type: 'modbus',
        },
      }

      expect(typeGroup?.keyFunction(testData)).toBe('modbus')
    })

    it('should extract category from data', () => {
      const catGroup = groupCatalog.find((e) => e.key === 'cat')
      const testData = {
        category: TreeEntity.BRIDGE,
        name: 'test-bridge',
        cat: 'test-category',
        payload: {
          id: 'test',
        },
      }

      expect(catGroup?.keyFunction(testData)).toBe('test-category')
    })

    it('should handle missing status gracefully', () => {
      const runtimeGroup = groupCatalog.find((e) => e.key === 'runtime')
      const testData: ClusterDataWrapper = {
        category: TreeEntity.ADAPTER,
        name: 'test-adapter',
        payload: {
          id: 'test',
          type: 'opcua',
        },
      }

      expect(runtimeGroup?.keyFunction(testData)).toBeUndefined()
    })
  })
})
