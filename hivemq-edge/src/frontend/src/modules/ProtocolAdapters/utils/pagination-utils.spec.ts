import { describe, expect } from 'vitest'
import { Status } from '@/api/__generated__'

import { compareStatus } from './pagination-utils.ts'

interface TestEachSuite {
  a: Status.connection | undefined
  b: Status.connection | undefined
  expected: 0 | 1 | -1
}

describe('compareStatus', () => {
  test.each<TestEachSuite>([
    { a: undefined, b: undefined, expected: 0 },
    { a: undefined, b: Status.connection.CONNECTED, expected: -1 },
    { a: Status.connection.STATELESS, b: undefined, expected: 1 },
    { a: Status.connection.CONNECTED, b: Status.connection.CONNECTED, expected: 0 },
    { a: Status.connection.CONNECTED, b: Status.connection.STATELESS, expected: -1 },
    { a: Status.connection.DISCONNECTED, b: Status.connection.CONNECTED, expected: 1 },
  ])('should returns $expected with $a and $b', ({ a, b, expected }) => {
    expect(compareStatus(a, b)).toStrictEqual(expected)
  })
})
