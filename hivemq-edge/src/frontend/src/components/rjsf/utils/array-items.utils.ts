const FORMAT_INDEX_MARKER = '#'
const FORMAT_SEPARATOR = '-'

import i18n from '@/config/i18n.config.ts'

export const formatItemName = (stub: string | undefined, index: number, description?: string) => {
  const token = stub || i18n.t('rjsf.ArrayFieldItem.item', { ns: 'components' })
  if (!description) return `${token} ${FORMAT_INDEX_MARKER}${index}`
  return `${token} ${FORMAT_INDEX_MARKER}${index} ${FORMAT_SEPARATOR} ${description}`
}
