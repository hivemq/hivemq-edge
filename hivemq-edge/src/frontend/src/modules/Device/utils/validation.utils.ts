import { FormValidation } from '@rjsf/utils'
import type { DomainTagList } from '@/api/__generated__'

export const customUniqueTagValidation =
  (allTags: string[]) => (formData: DomainTagList, errors: FormValidation<DomainTagList>) => {
    // initial names have already been checked and are excluded from the allTags

    // Check for duplicate names in the current form
    const allLocal = formData.items.map((tag) => tag.name)
    const localDuplicates = formData.items.reduce<number[]>((acc, tag, currentIndex) => {
      if (allLocal.indexOf(tag.name) !== currentIndex) {
        acc.push(currentIndex)
      }
      return acc
    }, [])

    for (const duplicate of localDuplicates) {
      errors?.items?.[duplicate]?.name?.addError('This tag name is already used on this device')
    }

    // Check for duplicate names across all devices
    const edgeDuplicates = formData.items.reduce<number[]>((acc, item, currentIndex) => {
      if (allTags.includes(item.name)) {
        acc.push(currentIndex)
      }
      return acc
    }, [])

    for (const duplicate of edgeDuplicates) {
      errors?.items?.[duplicate]?.name?.addError('This tag name is already used on another devices')
    }

    return errors
  }
