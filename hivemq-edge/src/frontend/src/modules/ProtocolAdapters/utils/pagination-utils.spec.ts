import { describe, expect } from 'vitest'
import { ConnectionStatus } from '@/api/__generated__'

import { compareStatus } from './pagination-utils.ts'

interface TestEachSuite {
  a: ConnectionStatus.status | undefined
  b: ConnectionStatus.status | undefined
  expected: 0 | 1 | -1
}

describe('compareStatus', () => {
  test.each<TestEachSuite>([
    { a: undefined, b: undefined, expected: 0 },
    { a: undefined, b: ConnectionStatus.status.CONNECTED, expected: -1 },
    { a: ConnectionStatus.status.CONNECTING, b: undefined, expected: 1 },
    { a: ConnectionStatus.status.CONNECTED, b: ConnectionStatus.status.CONNECTED, expected: 0 },
    { a: ConnectionStatus.status.CONNECTED, b: ConnectionStatus.status.CONNECTING, expected: -1 },
    { a: ConnectionStatus.status.DISCONNECTED, b: ConnectionStatus.status.CONNECTED, expected: 1 },
  ])('should returns $expected with $a and $b', ({ a, b, expected }) => {
    expect(compareStatus(a, b)).toStrictEqual(expected)
  })
})
