import { describe, expect } from 'vitest'
import { customUniqueTagValidation } from '@/modules/Device/utils/validation.utils.ts'
import { createErrorHandler } from '@rjsf/utils'
import type { DomainTagList } from '@/api/__generated__'

const mockAllTags = ['device1/tag1', 'device2/tag1', 'device2/tag2', 'device3/tag3']

describe('customUniqueTagValidation', () => {
  const validator = customUniqueTagValidation(mockAllTags)

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

  it('should detect duplication in every devices', () => {
    const formData: DomainTagList = {
      items: [
        { name: '1', definition: {} },
        { name: '1', definition: {} },
        { name: 'device2/tag1', definition: {} },
      ],
    }
    const results = validator(formData, createErrorHandler(formData))

    expect(results.items?.[1]?.name?.__errors).toStrictEqual(['This tag name is already used on this device'])
    expect(results.items?.[2]?.name?.__errors).toStrictEqual(['This tag name is already used on another devices'])
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
})
