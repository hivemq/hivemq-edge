import { describe, expect } from 'vitest'
import { customUniqueTagValidation, customUniqueTagInAdapterValidation } from '@/modules/Device/utils/validation.utils.ts'
import { createErrorHandler } from '@rjsf/utils'
import type { DomainTag, DomainTagList } from '@/api/__generated__'

describe('customUniqueTagValidation', () => {
  const validator = customUniqueTagValidation()

  it('should detect duplication in local device', () => {
    const formData: DomainTagList = {
      items: [
        { name: '1', definition: {} },
        { name: '1', definition: {} },
      ],
    }
    const results = validator(formData, createErrorHandler(formData))

    expect(results.items?.[1]?.name?.__errors).toStrictEqual(['This tag name is already used on this device'])
  })

  it('should detect no duplication', () => {
    const formData: DomainTagList = {
      items: [
        { name: '1', definition: {} },
        { name: '2', definition: {} },
        { name: 'device2/tag25', definition: {} },
      ],
    }
    const results = validator(formData, createErrorHandler(formData))

    expect(results.items?.[1]?.name?.__errors).toStrictEqual([])
    expect(results.items?.[2]?.name?.__errors).toStrictEqual([])
  })

  it('should allow the same tag name across different adapters', () => {
    // global uniqueness is NOT a requirement
    const formData: DomainTagList = {
      items: [{ name: 'boiler/temperature', definition: {} }],
    }
    const results = validator(formData, createErrorHandler(formData))

    expect(results.items?.[0]?.name?.__errors).toStrictEqual([])
  })
})

describe('customUniqueTagInAdapterValidation', () => {
  const existingNames = ['adapter/tag1', 'adapter/tag2']
  const validator = customUniqueTagInAdapterValidation(existingNames)

  it('should error when name already exists in the adapter', () => {
    const formData: DomainTag = { name: 'adapter/tag1', definition: {} }
    const results = validator(formData, createErrorHandler(formData))

    expect(results.name?.__errors).toStrictEqual(['This tag name is already used on this device'])
  })

  it('should not error on a unique name', () => {
    const formData: DomainTag = { name: 'adapter/tag3', definition: {} }
    const results = validator(formData, createErrorHandler(formData))

    expect(results.name?.__errors).toStrictEqual([])
  })

  it('should not error on an empty name', () => {
    const formData: DomainTag = { name: '', definition: {} }
    const results = validator(formData, createErrorHandler(formData))

    expect(results.name?.__errors).toStrictEqual([])
  })

  it('should not error when existingNames is empty', () => {
    const freshValidator = customUniqueTagInAdapterValidation([])
    const formData: DomainTag = { name: 'adapter/tag1', definition: {} }
    const results = freshValidator(formData, createErrorHandler(formData))

    expect(results.name?.__errors).toStrictEqual([])
  })

  it('should not error when formData is undefined', () => {
    const results = validator(undefined, createErrorHandler({}))

    expect(results.__errors).toStrictEqual([])
  })
})
