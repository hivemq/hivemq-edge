// types for it.each and describe.each
// any help improving them is welcome
// https://github.com/bahmutov/cypress-each

type TestTitleFn<T> = (item: T, index: number, items: T[]) => string
type ItemPredicateFunction<T> = (item: T, index: number, items: T[]) => boolean

type TestCaseObject3<T0, T1, T2> = {
  [index: string]: [T0, T1, T2]
}

type TestCaseObject2<T0, T1> = {
  [index: string]: [T0, T1]
}

type TestCaseObject<T> = {
  [index: string]: T
}

declare namespace Mocha {
  type TestCallback<T extends readonly any[]> = T extends []
    ? (this: Context, arg1: any, arg2: any) => void
    : Parameters<(...res: [...T, any, any]) => void> extends [...infer R]
    ? R extends readonly [...T, any, any]
      ? (this: Context, ...res: [...R]) => void
      : never
    : never
  type TestCallback1<T0> = (this: Context, arg0: T0) => void
  type TestCallback2<T0, T1> = (this: Context, arg0: T0, arg1: T1) => void
  type TestCallback3<T0, T1, T2> = (
    this: Context,
    arg0: T0,
    arg1: T1,
    arg2: T2,
  ) => void

  interface TestFunction {
    /**
     * Iterates over each given item (optionally chunked), and creates
     * a separate test for each one.
     * @param values Input items to create the tests form OR number of times to repeat a test
     * @param totalChunks (Optional) number of chunks to split the items into, or Nth filter, or a predicate function
     * @param chunkIndex (Optional) index of the chunk to get items from
     * @example it.each([1, 2, 3])('test %K', (x) => ...)
     * @see https://github.com/bahmutov/cypress-each
     */
    each<T extends readonly [...T]>(
      values: Array<readonly [...T]>,
      totalChunks?: number,
      chunkIndex?: number,
    ): (
      titlePattern: string | TestTitleFn<[...T]>,
      fn: TestCallback<[...T]>,
    ) => void
    each<T = unknown>(
      values: T[] | number,
      totalChunks?: number | ItemPredicateFunction<T>,
      chunkIndex?: number,
    ): (titlePattern: string | TestTitleFn<T>, fn: TestCallback<[T]>) => void

    /**
     * A single test case object where the keys are test titles,
     * and the values are used as inputs to the test callback
     * @see https://github.com/bahmutov/cypress-each#test-case-object
     * @example
     *  const testCases = {
     *    // key: the test label
     *    // value: list of inputs for each test case
     *    'positive numbers': [1, 6, 7], // [a, b, expected result]
     *    'negative numbers': [1, -6, -5],
     *  }
     *  it.each(testCases)((a, b, result) => { ... })
     */
    each<T0, T1, T2>(
      testCases: TestCaseObject3<T0, T1, T2>,
    ): (fn: TestCallback3<T0, T1, T2>) => void

    /**
     * A single test case object where the keys are test titles,
     * and the values are used as inputs to the test callback
     * @see https://github.com/bahmutov/cypress-each#test-case-object
     * @example
     *  const testCases = {
     *    // key: the test label
     *    // value: list of inputs for each test case
     *    'positive numbers': [1, 6, 7], // [a, b, expected result]
     *    'negative numbers': [1, -6, -5],
     *  }
     *  it.each(testCases)((a, b, result) => { ... })
     */
    each<T0, T1>(
      testCases: TestCaseObject2<T0, T1>,
    ): (fn: TestCallback2<T0, T1>) => void

    /**
     * A single test case object where the keys are test titles,
     * and the single value are used as inputs to the test callback
     * @see https://github.com/bahmutov/cypress-each#test-case-object
     * @example
     *  const testCases = {
     *    'two': 2,
     *    'three': 3,
     *  }
     *  it.each(testCases)((a) => { ... })
     */
    each<T0>(testCases: TestCaseObject<T0>): (fn: TestCallback1<T0>) => void
  }

  interface SuiteFunction {
    /**
     * Iterates over each given item (optionally chunked), and creates
     * a separate suite for each one.
     * @param values Input items to create the tests form
     * @param totalChunks (Optional) number of chunks to split the items into
     * @param chunkIndex (Optional) index of the chunk to get items from
     * @example describe.each([1, 2, 3])('suite %K', (item) => ...)
     * @see https://github.com/bahmutov/cypress-each
     */
    each<T = unknown>(
      values: T[] | number,
      totalChunks?: number | ItemPredicateFunction<T>,
      chunkIndex?: number,
    ): (titlePattern: string | TestTitleFn<T>, fn: TestCallback<[T]>) => void
  }
}
