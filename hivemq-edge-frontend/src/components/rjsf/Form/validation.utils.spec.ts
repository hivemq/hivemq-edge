import { describe, expect, it } from 'vitest'
import type { ErrorObject } from '@/components/rjsf/Form/validation.utils.ts'
import {
  customFormatsValidator,
  customLocalizer,
  validationTag,
  validationTopic,
  validationTopicFilter,
} from '@/components/rjsf/Form/validation.utils.ts'
type FormatValidator<T extends string | number> = (data: T) => boolean

enum Messages {
  noEmptyString = 'No empty string',
  noNullChar = 'No null character',
  noWildCards = 'Wildcards + and # are not allowed',
  noWildCardsSharedName = 'Wildcards + and # are not allowed in a shared name',
  noEmptySharedName = 'Shared name cannot be empty',
  noMultiLevelString = 'Wildcard # must not be followed or preceded by any character',
  noSingleLevelString = 'Wildcard + must not be followed or preceded by any character',
  noSingleLevelFinal = 'Wildcard + must be followed by the end-of-segment `/`',
}

interface ValidationTestSuite {
  entity: string
  expected: Messages | undefined
}

describe('validationRules', () => {
  describe('validationTopic', () => {
    test.each<ValidationTestSuite>([
      { entity: '', expected: Messages.noEmptyString },
      { entity: 'real/topic/\u0000', expected: Messages.noNullChar },
      { entity: 'real/topic/#', expected: Messages.noWildCards },
      { entity: 'real/+/topic', expected: Messages.noWildCards },
      { entity: 'real/+/\u0000/topic', expected: Messages.noNullChar },
      { entity: 'real/\u0000/+/topic', expected: Messages.noNullChar },
      { entity: 'real/topic', expected: undefined },
    ])('should return $expected  with $entity', ({ entity, expected }) => {
      expect(validationTopic(entity)).toBe(expected)
    })
  })

  describe('validationTag', () => {
    test.each<ValidationTestSuite>([
      { entity: '', expected: Messages.noEmptyString },
      { entity: 'real/topic/\u0000', expected: Messages.noNullChar },
      { entity: 'real/topic/#', expected: Messages.noWildCards },
      { entity: 'real/+/topic', expected: Messages.noWildCards },
      { entity: 'real/+/\u0000/topic', expected: Messages.noNullChar },
      { entity: 'real/\u0000/+/topic', expected: Messages.noNullChar },
      { entity: 'real/topic', expected: undefined },
    ])('should return $expected  with $entity', ({ entity, expected }) => {
      expect(validationTag(entity)).toBe(expected)
    })
  })

  describe('validationTopicFilter', () => {
    test.each<ValidationTestSuite>([
      { entity: '', expected: Messages.noEmptyString },
      { entity: 'real/\u0000/topic', expected: Messages.noNullChar },
      { entity: '$share/re+al/topic', expected: Messages.noWildCardsSharedName },
      { entity: '$share//topic', expected: Messages.noEmptySharedName },
      { entity: 'real/#/topic', expected: Messages.noMultiLevelString },
      { entity: 'real/wrong#/topic', expected: Messages.noMultiLevelString },
      { entity: 'real/#wrong/topic', expected: Messages.noMultiLevelString },

      { entity: 'real/wrong+/topic', expected: Messages.noSingleLevelString },
      { entity: 'real/+wrong/topic', expected: Messages.noSingleLevelFinal },

      { entity: '$share/sss/topic', expected: undefined },
      { entity: 'real/+/topic', expected: undefined },
      { entity: 'real/topic/#', expected: undefined },
    ])('should return $expected  with $entity', ({ entity, expected }) => {
      expect(validationTopicFilter(entity)).toBe(expected)
    })
  })
})

const MOCK_ERRORS: ErrorObject[] = [
  {
    instancePath: '/items/0',
    schemaPath: '#/properties/items/items/required',
    keyword: 'required',
    params: {
      missingProperty: 'tag',
    },
    message: "must have required property 'tag'",
    schema: ['tag'],
  },
  {
    instancePath: '/items/1/tag',
    schemaPath: '#/properties/items/items/properties/tag/format',
    keyword: 'format',
    params: {
      format: 'mqtt-tag',
    },
    message: 'must match format "mqtt-tag"',
    schema: 'mqtt-tag',
    data: 'dd+',
  },
  {
    instancePath: '/items/1/tag',
    schemaPath: '#/properties/items/items/properties/tag/format',
    keyword: 'format',
    params: {
      format: 'mqtt-topic',
    },
    message: 'must match format "mqtt-topic"',
    schema: 'mqtt-topic',
    data: 'dd+',
  },
  {
    instancePath: '/items/1/tag',
    schemaPath: '#/properties/items/items/properties/tag/format',
    keyword: 'format',
    params: {
      format: 'mqtt-topic-filter',
    },
    message: 'must match format "mqtt-topic-filter"',
    schema: 'mqtt-topic-filter',
    data: 'dd+/d#',
  },
]

describe('customLocalizer', () => {
  it('should return a warning when no data schema given', () => {
    const errors: ErrorObject[] = [...MOCK_ERRORS]

    customLocalizer(errors)
    expect(errors).toStrictEqual([
      expect.objectContaining({ message: "must have required property 'tag'" }),
      expect.objectContaining({ message: Messages.noWildCards }),
      expect.objectContaining({ message: Messages.noWildCards }),
      expect.objectContaining({ message: Messages.noSingleLevelString }),
    ])
  })
})

describe('customFormatsValidator', () => {
  it('should return a warning when no data schema given', () => {
    const {
      ajv: { formats },
    } = customFormatsValidator
    const {
      identifier,
      boolean,
      'mqtt-topic': mqttTopic,
      'mqtt-tag': mqttTag,
      'mqtt-topic-filter': mqttTopicFilter,
    } = formats

    expect((boolean as FormatValidator<string>)('test')).toBeDefined()
    expect((mqttTopic as FormatValidator<string>)('test')).toBeDefined()
    expect((mqttTag as FormatValidator<string>)('test')).toBeDefined()
    expect((mqttTopicFilter as FormatValidator<string>)('test')).toBeDefined()
    expect((identifier as FormatValidator<string>)('test')).toBeDefined()
  })
})
