import type { HTMLContent } from '@tiptap/react'

export const parseInterpolations = (value: string | undefined): HTMLContent => {
  const replacer = (_: string, ...args: (string | number)[]) => `<span data-type="mention" data-id="${args[0]}"></span>`

  return value?.replaceAll(/\${([a-zA-Z]*)}/g, replacer) || ''
}
