import type { MutableRefObject } from 'react'
import type Form from '@rjsf/core'
import type { RJSFSchema, RJSFValidationError, UiSchema } from '@rjsf/utils'
import type { ChakraRJSFormContext, UITab, UITabIndexed } from '@/components/rjsf/Form/types.ts'

export const _toPath = (path: string) => path?.match(/([^[.\]])+/g)

// From RJSF
/* istanbul ignore next -- @preserve */
const focusOnError = (formElement: HTMLFormElement, error: RJSFValidationError) => {
  // const { idPrefix = 'root', idSeparator = '_' } = props;
  const idPrefix = 'root',
    idSeparator = '_'

  const { property } = error
  if (!property) return

  const path = _toPath(property)
  if (!path) return
  if (path[0] === '') {
    // Most of the time the `.foo` property results in the first element being empty, so replace it with the idPrefix
    path[0] = 'root'
  } else {
    // Otherwise insert the idPrefix into the first location using unshift
    path.unshift(idPrefix)
  }

  const elementId = path.join(idSeparator)
  let field = formElement.querySelector(`#${elementId}`) as HTMLElement

  if (!field) {
    // if not an exact match, try finding an input starting with the element id (like radio buttons or checkboxes)
    field = formElement.querySelector(`input[id^="${elementId}"`) as HTMLElement
  }
  if (field instanceof HTMLDivElement) {
    // if the field is a select or other wrapper, find the nested input
    field = formElement.querySelector<HTMLDivElement>(`#${elementId} input`) as HTMLElement
  }

  if (field) {
    field.focus()
  }
}

/* istanbul ignore next -- @preserve */
export const customFocusError = (wrapperRef: MutableRefObject<null>) => (error: RJSFValidationError) => {
  if (!wrapperRef.current) return

  const rjsForm = wrapperRef.current as Form

  focusOnError(rjsForm.formElement.current, error)
}

export const isNumeric = (element: string) => /^\d+$/.test(element)
export const deepGet = (obj: object, keys: (string | number)[]) => {
  if (!keys.length) return null

  return keys.reduce((xs, x) => xs?.[x as keyof typeof xs] ?? null, obj)
}

export const isPropertyBehindCollapsedElement = (
  property: string,
  uiSchema: UiSchema<unknown, RJSFSchema, ChakraRJSFormContext>
) => {
  const path = property.split('.')
  path?.shift()
  if (path.length) {
    // assume only the first array matters. Might need to check nested arrays
    const foundIndex = path.findIndex(isNumeric)
    if (foundIndex != -1) {
      const root = path.slice(0, foundIndex)
      root.push('items')
      const item = deepGet(uiSchema, root)
      if (item) {
        // @ts-ignore Type will need to be corrected
        const { 'ui:collapsable': isCollapsible } = item
        if (isCollapsible) {
          return ['root', ...path.slice(0, foundIndex + 1)]
        }
      }
    }
  }
  return undefined
}

export const isPropertyBehindTab = (
  property: string,
  uiSchema: UiSchema<unknown, RJSFSchema, ChakraRJSFormContext>
) => {
  const { 'ui:tabs': tabs } = uiSchema
  if (!Array.isArray(tabs)) return undefined

  const root = property.split('.')[1] || property
  if (!root) return undefined

  let inTab: UITabIndexed | null = null
  for (const [index, tab] of (tabs as UITab[]).entries()) {
    const { properties } = tab
    if (properties && properties.includes(root)) inTab = { ...tab, index }
  }

  return inTab ? inTab : undefined
}
