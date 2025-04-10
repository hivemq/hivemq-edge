import { expect } from 'vitest'
import { type FlatJSONSchema7 } from '@/components/rjsf/MqttTransformation/utils/json-schema.utils.ts'
import {
  filterSupportedProperties,
  formatPath,
  fromJsonPath,
  isMappingSupported,
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
