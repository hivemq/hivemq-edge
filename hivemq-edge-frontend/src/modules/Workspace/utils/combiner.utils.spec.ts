import { expect } from 'vitest'
import { arrayContains, arrayWithSameObjects, objectsEqual } from './combiner.utils'

interface Test {
  name?: string
  age?: number
  fake?: boolean
}

const test1: Test = { name: 'John', age: 33 }
const test2: Test = { age: 33, name: 'John' }
const test3: Test = { name: 'John', age: 45 }
const test4: Test = { name: 'John', age: 33, fake: true }

interface ObjectsEqualSuite<T> {
  rule: string
  obj1: T
  obj2: T
  expected: boolean
}

const objectsEqualSuiteTests: ObjectsEqualSuite<Test>[] = [
  { rule: 'same object', obj1: test1, obj2: test1, expected: true },
  { rule: 'different order', obj1: test1, obj2: test2, expected: true },
  { rule: 'different objects', obj1: test1, obj2: test3, expected: false },
  { rule: 'empty objects', obj1: {}, obj2: {}, expected: true },
  { rule: 'different keys', obj1: test1, obj2: test4, expected: false },
]

describe('objectsEqual', () => {
  test.each<ObjectsEqualSuite<Test>>(objectsEqualSuiteTests)(
    '$rule should return $expected',
    ({ obj1, obj2, expected }) => {
      expect(objectsEqual(obj1, obj2)).toStrictEqual(expected)
    }
  )
})

interface ArrayContainsSuite<T> {
  rule: string
  array: T[]
  obj: T
  expected: boolean
}

const arrayContainsSuiteTests: ArrayContainsSuite<Test>[] = [
  { rule: 'same object1', array: [test1], obj: test1, expected: true },
  { rule: 'same object22', array: [test2], obj: test1, expected: true },
  { rule: 'same object3', array: [], obj: test1, expected: false },
  { rule: 'same object33', array: [test3, test4], obj: test1, expected: false },
]

describe('arrayContains', () => {
  test.each<ArrayContainsSuite<Test>>(arrayContainsSuiteTests)(
    '$rule should return $expected',
    ({ array, obj, expected }) => {
      expect(arrayContains(array, obj)).toStrictEqual(expected)
    }
  )
})

interface ArraysSuite<T> {
  rule: string
  array1: T[]
  array2: T[]
  expected: T[] | undefined
}

const arraysSuiteTests: ArraysSuite<Test>[] = [
  { rule: 'empty array', array1: [], array2: [], expected: [] },
  { rule: 'same objects', array1: [test1], array2: [test1], expected: [test1] },
  { rule: 'similar objects', array1: [test1], array2: [test2], expected: [test1] },
  { rule: 'different objects', array1: [test1], array2: [test3], expected: undefined },
  { rule: 'more objects', array1: [test1, test3], array2: [test1], expected: undefined },
  { rule: 'less objects', array1: [test1], array2: [test1, test3], expected: undefined },
]

describe('arrayWithSameObjects', () => {
  test.each<ArraysSuite<Test>>(arraysSuiteTests)('$rule should return $expected', ({ array1, array2, expected }) => {
    expect(arrayWithSameObjects(array1)(array2)).toStrictEqual(expected)
  })
})
