import { expect } from 'vitest'
import { formatTopicString } from '@/components/MQTT/topic-utils.ts'

interface TestEachSuite {
  topic: string
  expected: string
}

describe('formatTopicString', () => {
  it.each<TestEachSuite>([
    { topic: '', expected: '' },
    { topic: '12345', expected: '12345' },
    { topic: '123/45', expected: '123 / 45' },
    { topic: '12/3/45', expected: '12 / 3 / 45' },
    { topic: '12/3/45/', expected: '12 / 3 / 45 / ' },
  ])('should returns $expected with $topic', ({ topic, expected }) => {
    expect(formatTopicString(topic)).toStrictEqual(expected)
  })
})
