import { expect } from 'vitest'
import levenshtein from '@/components/rjsf/BatchSubscription/utils/levenshtein.utils.ts'

describe('Levenshtein', () => {
  it.each([
    ['a', 'b', 1],
    ['ab', 'ac', 1],
    ['ac', 'bc', 1],
    ['abc', 'axc', 1],
    ['kitten', 'sitting', 3],
    ['xabxcdxxefxgx', '1ab2cd34ef5g6', 6],
    ['cat', 'cow', 2],
    ['xabxcdxxefxgx', 'abcdefg', 6],
    ['javawasneat', 'scalaisgreat', 7],
    ['example', 'samples', 3],
    ['sturgeon', 'urgently', 6],
    ['levenshtein', 'frankenstein', 6],
    ['distance', 'difference', 5],
    ['因為我是中國人所以我會說中文', '因為我是英國人所以我會說英文', 2],
  ])('should calculate the distance between %s and %s as %s', (a, b, expected) => {
    expect(levenshtein(a, b)).toBe(expected)
  })
})
