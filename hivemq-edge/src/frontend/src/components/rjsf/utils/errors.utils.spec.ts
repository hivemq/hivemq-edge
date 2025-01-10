import { describe, expect } from 'vitest'
import { hasNestedError } from '@/components/rjsf/utils/errors.utils.ts'
import { ErrorSchema } from '@rjsf/utils'

interface TestProperty {
  test?: object
  nested?: {
    test1?: object
    test2?: object
    test3?: object
  }
}

const errorGenerator = (msg: undefined | string[], g = false): ErrorSchema<TestProperty> => {
  return {
    __errors: g ? undefined : msg,
    nested: {
      __errors: g ? undefined : msg,
      test3: { __errors: g ? undefined : msg },
      test1: { __errors: msg },
    },
  }
}

interface TestEachSuite {
  schema: ErrorSchema<TestProperty> | undefined
  hasError: boolean
}

const allTests: TestEachSuite[] = [
  { schema: undefined, hasError: false },
  { schema: { __errors: undefined }, hasError: false },
  { schema: { __errors: [] }, hasError: false },
  { schema: { __errors: ['This is an error'] }, hasError: true },
  { schema: errorGenerator(undefined), hasError: false },
  { schema: errorGenerator([]), hasError: false },
  { schema: errorGenerator(['This is an error']), hasError: true },
  { schema: errorGenerator(['This is an error'], true), hasError: true },
]

describe('hasNestedError', () => {
  it.each<TestEachSuite>(allTests)('should returns $expected with $schema', ({ schema, hasError }) => {
    expect(hasNestedError<TestProperty>(schema)).toStrictEqual(hasError)
  })
})
