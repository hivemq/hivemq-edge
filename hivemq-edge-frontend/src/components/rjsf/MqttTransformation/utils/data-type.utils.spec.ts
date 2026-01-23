import { expect } from 'vitest'
import type { Instruction } from '@/api/__generated__'
import { type FlatJSONSchema7 } from '@/components/rjsf/MqttTransformation/utils/json-schema.utils.ts'
import {
  filterReadOnlyInstructions,
  filterSupportedProperties,
  formatPath,
  fromJsonPath,
  isMappingSupported,
  isReadOnly,
  toJsonPath,
} from '@/components/rjsf/MqttTransformation/utils/data-type.utils.ts'

const MOCK_PROPERTY: FlatJSONSchema7 = {
  description: undefined,
  path: [],
  key: 'billing-address',
  title: 'Billing address',
}

describe('isMappingSupported', () => {
  it('should tell when a property type is supported ', async () => {
    expect(isMappingSupported({ ...MOCK_PROPERTY })).toBeFalsy()
    expect(isMappingSupported({ ...MOCK_PROPERTY, type: 'object' })).toBeFalsy()

    expect(isMappingSupported({ ...MOCK_PROPERTY, type: 'string' })).toBeTruthy()
    expect(isMappingSupported({ ...MOCK_PROPERTY, type: 'number' })).toBeTruthy()
    expect(isMappingSupported({ ...MOCK_PROPERTY, type: 'boolean' })).toBeTruthy()
    expect(isMappingSupported({ ...MOCK_PROPERTY, type: 'null' })).toBeTruthy()
    expect(isMappingSupported({ ...MOCK_PROPERTY, type: 'array' })).toBeTruthy()
    expect(isMappingSupported({ ...MOCK_PROPERTY, type: 'integer' })).toBeTruthy()
  })
})

describe('filterSupportedProperties', () => {
  it('should tell when a property should be returned', async () => {
    expect(filterSupportedProperties({ ...MOCK_PROPERTY })).toBeTruthy()
    expect(filterSupportedProperties({ ...MOCK_PROPERTY, path: ['object', 'subProperty'] })).toBeFalsy()
  })
})

describe('formatPath', () => {
  it('should tell when a property should be returned', async () => {
    expect(formatPath('')).toStrictEqual('')
    expect(formatPath('string')).toStrictEqual('string')
    expect(formatPath('path.to.string')).toStrictEqual('path.​to.​string')
    expect(formatPath('path.to.string')).not.toStrictEqual('path. to. string')
  })
})

describe('toJsonPath', () => {
  it('should tell when a property should be returned', async () => {
    expect(toJsonPath('')).toStrictEqual('$')
    expect(toJsonPath('$.123')).toStrictEqual('$.123')
    expect(toJsonPath('123')).toStrictEqual('$.123')
  })
})

describe('fromJsonPath', () => {
  it('should tell when a property should be returned', async () => {
    expect(fromJsonPath('')).toStrictEqual('')
    expect(fromJsonPath('$')).toStrictEqual('')
    expect(fromJsonPath('$.123')).toStrictEqual('123')
    expect(fromJsonPath('123')).toStrictEqual('123')
  })
})

describe('isReadOnly', () => {
  it('should return true when property has readOnly: true', () => {
    expect(isReadOnly({ ...MOCK_PROPERTY, readOnly: true })).toBe(true)
  })

  it('should return false when property has readOnly: false', () => {
    expect(isReadOnly({ ...MOCK_PROPERTY, readOnly: false })).toBe(false)
  })

  it('should return false when property has no readOnly property', () => {
    expect(isReadOnly({ ...MOCK_PROPERTY })).toBe(false)
  })
})

describe('filterReadOnlyInstructions', () => {
  const MOCK_PROPERTIES: FlatJSONSchema7[] = [
    { key: 'id', path: [], type: 'string', readOnly: true },
    { key: 'name', path: [], type: 'string' },
    { key: 'version', path: ['config'], type: 'string', readOnly: true },
    { key: 'setting', path: ['config'], type: 'number' },
  ]

  const MOCK_INSTRUCTIONS: Instruction[] = [
    { source: '$.source1', destination: '$.id' },
    { source: '$.source2', destination: '$.name' },
    { source: '$.source3', destination: '$.config.version' },
    { source: '$.source4', destination: '$.config.setting' },
  ]

  it('should return empty array when instructions is undefined', () => {
    expect(filterReadOnlyInstructions(undefined, MOCK_PROPERTIES)).toStrictEqual([])
  })

  it('should return empty array when instructions is empty', () => {
    expect(filterReadOnlyInstructions([], MOCK_PROPERTIES)).toStrictEqual([])
  })

  it('should return all instructions when no properties are readonly', () => {
    const writableProperties: FlatJSONSchema7[] = [
      { key: 'name', path: [], type: 'string' },
      { key: 'setting', path: ['config'], type: 'number' },
    ]
    const instructions: Instruction[] = [
      { source: '$.source1', destination: '$.name' },
      { source: '$.source2', destination: '$.config.setting' },
    ]

    expect(filterReadOnlyInstructions(instructions, writableProperties)).toStrictEqual(instructions)
  })

  it('should filter out instructions targeting readonly properties', () => {
    const result = filterReadOnlyInstructions(MOCK_INSTRUCTIONS, MOCK_PROPERTIES)

    expect(result).toHaveLength(2)
    expect(result).toContainEqual({ source: '$.source2', destination: '$.name' })
    expect(result).toContainEqual({ source: '$.source4', destination: '$.config.setting' })
  })

  it('should not include instructions targeting readonly root properties', () => {
    const result = filterReadOnlyInstructions(MOCK_INSTRUCTIONS, MOCK_PROPERTIES)

    expect(result).not.toContainEqual(expect.objectContaining({ destination: '$.id' }))
  })

  it('should not include instructions targeting readonly nested properties', () => {
    const result = filterReadOnlyInstructions(MOCK_INSTRUCTIONS, MOCK_PROPERTIES)

    expect(result).not.toContainEqual(expect.objectContaining({ destination: '$.config.version' }))
  })
})
