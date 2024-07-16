import { describe, expect } from 'vitest'
import { parseHotkey } from '@datahub/utils/hotkeys.utils.ts'

interface ParseHotkeySuite {
  hotKey: string
  expected: KeyboardEventInit
}

describe('parseHotkey', () => {
  test.each<ParseHotkeySuite>([
    {
      hotKey: 'undefined',
      expected: { key: 'undefined' },
    },
    {
      hotKey: 'Meta+C',
      expected: { key: 'c', metaKey: true },
    },
    {
      hotKey: 'Shift+FAKE',
      expected: { key: 'fake', shiftKey: true },
    },
  ])('should return a keydown for $hotKey ', ({ hotKey, expected }) => {
    expect(parseHotkey(hotKey)).toStrictEqual(expect.objectContaining({ ...expected }))
  })
})
