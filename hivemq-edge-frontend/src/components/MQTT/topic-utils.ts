import type { DataIdentifierReference } from '@/api/__generated__'

const TOPIC_SEPARATOR = ' / '
const OWNERSHIP_SEPARATOR = ' :: '

export const formatTopicString = (topic: string) => topic.split('/').join(TOPIC_SEPARATOR)

/**
 * Returns the ownership label for a data identifier reference: scope + OWNERSHIP_SEPARATOR + id
 * when a scope is present, or just id when it is absent.
 */
export const formatOwnershipString = (ref: DataIdentifierReference): string =>
  ref.scope ? `${ref.scope}${OWNERSHIP_SEPARATOR}${ref.id}` : ref.id
