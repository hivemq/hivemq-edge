import type { HTMLContent } from '@tiptap/react'
import type { MentionNodeAttrs } from '@datahub/components/interpolation/SuggestionList.tsx'

export const parseInterpolations = (value: string | undefined): HTMLContent => {
  const replacer = (_: string, ...args: (string | number)[]) => `<span data-type="mention" data-id="${args[0]}"></span>`

  return value?.replaceAll(/\${([a-zA-Z]*)}/g, replacer) || ''
}

export const getItems = (query: string) => {
  // TODO[NVL] Get the list from the datahub API
  return ['clientId', 'policyId', 'fromState', 'toState', 'validationResult', 'triggerEvent', 'timestamp', 'topic']
    .map<MentionNodeAttrs>((name, index) => ({ label: name, id: index.toString() }))
    .filter((item) => item.label.toLowerCase().startsWith(query.toLowerCase()))
}
