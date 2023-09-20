import { describe, expect } from 'vitest'
import { Status } from '@/api/__generated__'

import { compareStatus } from './pagination-utils.ts'

interface TestEachSuite {
  a: Status.connectionStatus | undefined
  b: Status.connectionStatus | undefined
  expected: 0 | 1 | -1
}

describe('compareStatus', () => {
  test.each<TestEachSuite>([
    { a: undefined, b: undefined, expected: 0 },
    { a: undefined, b: Status.connectionStatus.CONNECTED, expected: -1 },
    { a: Status.connectionStatus.STATELESS, b: undefined, expected: 1 },
    { a: Status.connectionStatus.CONNECTED, b: Status.connectionStatus.CONNECTED, expected: 0 },
    { a: Status.connectionStatus.CONNECTED, b: Status.connectionStatus.STATELESS, expected: -1 },
    { a: Status.connectionStatus.DISCONNECTED, b: Status.connectionStatus.CONNECTED, expected: 1 },
  ])('should returns $expected with $a and $b', ({ a, b, expected }) => {
    expect(compareStatus(a, b)).toStrictEqual(expected)
  })
})
