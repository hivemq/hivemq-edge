import { expect } from 'vitest'
import { validateDuplicates } from './rjsf.utils.ts'

describe('getDuplicates', () => {
  it('should return the indexes of duplicated items', async () => {
    expect(validateDuplicates([])).toHaveLength(0)
    expect(validateDuplicates(['1', '2', '3'])).toHaveLength(0)
    expect(validateDuplicates(['1', '2', '3', '2'])).toHaveLength(1)

    expect(validateDuplicates(['1', '2', '3', '2', '1'])).toHaveLength(2)
    expect(validateDuplicates(['1', '2', '3', '2', '1'])).toStrictEqual(
      new Map([
        ['1', [0, 4]],
        ['2', [1, 3]],
      ])
    )
  })
})
