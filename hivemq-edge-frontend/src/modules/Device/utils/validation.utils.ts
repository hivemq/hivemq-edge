import type { FormValidation } from '@rjsf/utils'
import type { DomainTag, DomainTagList } from '@/api/__generated__'

import i18n from '@/config/i18n.config.ts'

export const customUniqueTagValidation = () => (formData: DomainTagList, errors: FormValidation<DomainTagList>) => {
  const allLocal = formData.items.map((tag) => tag.name)
  const localDuplicates = formData.items.reduce<number[]>((acc, tag, currentIndex) => {
    if (allLocal.indexOf(tag.name) !== currentIndex) {
      acc.push(currentIndex)
    }
    return acc
  }, [])

  for (const duplicate of localDuplicates) {
    errors?.items?.[duplicate]?.name?.addError(i18n.t('validation.identifier.tag.uniqueDevice', { ns: 'translation' }))
  }

  return errors
}

export const customUniqueTagInAdapterValidation =
  (existingNames: string[]) => (formData: DomainTag | undefined, errors: FormValidation<DomainTag>) => {
    if (!formData?.name) return errors
    if (existingNames.includes(formData.name)) {
      errors.name?.addError(i18n.t('validation.identifier.tag.uniqueDevice', { ns: 'translation' }))
    }
    return errors
  }
