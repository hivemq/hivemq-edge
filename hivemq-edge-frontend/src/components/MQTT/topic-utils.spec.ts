import { expect } from 'vitest'
import { DataIdentifierReference } from '@/api/__generated__'
import { formatOwnershipString, formatTopicString } from '@/components/MQTT/topic-utils.ts'

interface FormatTopicSuite {
  topic: string
  expected: string
}

describe('formatTopicString', () => {
  it.each<FormatTopicSuite>([
    { topic: '', expected: '' },
    { topic: '12345', expected: '12345' },
    { topic: '123/45', expected: '123 / 45' },
    { topic: '12/3/45', expected: '12 / 3 / 45' },
    { topic: '12/3/45/', expected: '12 / 3 / 45 / ' },
    // Ownership-prefixed strings: the OWNERSHIP_SEPARATOR must pass through untouched;
    // only path slashes in the id segment are formatted.
    { topic: 'my-adapter :: my/tag/t1', expected: 'my-adapter :: my / tag / t1' },
    { topic: 'opcua-adapter :: a/b/c/d', expected: 'opcua-adapter :: a / b / c / d' },
  ])('should return $expected with $topic', ({ topic, expected }) => {
    expect(formatTopicString(topic)).toStrictEqual(expected)
  })
})

interface OwnershipSuite {
  ref: DataIdentifierReference
  expected: string
}

describe('formatOwnershipString', () => {
  it.each<OwnershipSuite>([
    {
      ref: { id: 'my/tag/t1', type: DataIdentifierReference.type.TAG, scope: 'my-adapter' },
      expected: 'my-adapter :: my/tag/t1',
    },
    {
      ref: { id: 'my/tag/t1', type: DataIdentifierReference.type.TAG, scope: null },
      expected: 'my/tag/t1',
    },
    {
      ref: { id: 'my/tag/t1', type: DataIdentifierReference.type.TAG, scope: undefined },
      expected: 'my/tag/t1',
    },
    {
      ref: {
        id: 'industrial/plant/floor2/machine-a/sensor-12',
        type: DataIdentifierReference.type.TAG,
        scope: 'opcua-production-adapter',
      },
      expected: 'opcua-production-adapter :: industrial/plant/floor2/machine-a/sensor-12',
    },
    {
      ref: { id: 'factory/+/sensors/temperature', type: DataIdentifierReference.type.TOPIC_FILTER, scope: null },
      expected: 'factory/+/sensors/temperature',
    },
    {
      ref: { id: 'asset/pump-station-a', type: DataIdentifierReference.type.PULSE_ASSET, scope: null },
      expected: 'asset/pump-station-a',
    },
  ])('should return "$expected" for scope=$ref.scope id=$ref.id', ({ ref, expected }) => {
    expect(formatOwnershipString(ref)).toStrictEqual(expected)
  })
})
