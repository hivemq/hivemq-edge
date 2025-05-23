import { describe, expect } from 'vitest'
import { Event, Status } from '@/api/__generated__'

import { compareSeverity, compareStatus } from './pagination-utils.ts'

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

interface TestSeveritySuite {
  a: Event.severity | undefined
  b: Event.severity | undefined
  expected: 0 | 1 | -1
}

describe('compareSeverity', () => {
  test.each<TestSeveritySuite>([
    { a: undefined, b: undefined, expected: 0 },
    { a: undefined, b: Event.severity.INFO, expected: -1 },
    { a: Event.severity.INFO, b: undefined, expected: 1 },
    { a: Event.severity.INFO, b: Event.severity.INFO, expected: 0 },
    { a: Event.severity.INFO, b: Event.severity.WARN, expected: -1 },
    { a: Event.severity.INFO, b: Event.severity.ERROR, expected: -1 },
    { a: Event.severity.INFO, b: Event.severity.CRITICAL, expected: -1 },
    { a: Event.severity.WARN, b: Event.severity.INFO, expected: 1 },
    { a: Event.severity.ERROR, b: Event.severity.INFO, expected: 1 },
    { a: Event.severity.CRITICAL, b: Event.severity.INFO, expected: 1 },
  ])('should returns $expected with $a and $b', ({ a, b, expected }) => {
    expect(compareSeverity(a, b)).toStrictEqual(expected)
  })
})
