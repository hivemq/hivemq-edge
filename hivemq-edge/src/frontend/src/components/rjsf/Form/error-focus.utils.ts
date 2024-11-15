import { MutableRefObject } from 'react'
import Form from '@rjsf/core'
import { RJSFValidationError } from '@rjsf/utils'

// From RJSF
const focusOnError = (formElement: HTMLFormElement, error: RJSFValidationError) => {
  // const { idPrefix = 'root', idSeparator = '_' } = props;
  const idPrefix = 'root',
    idSeparator = '_'

  const { property } = error
  if (!property) return

  // WARNING: This is not a drop in replacement solution and
  // it might not work for some edge cases. Test your code!
  const _toPath = (path: string) => path?.match(/([^[.\]])+/g)

  const path = _toPath(property)
  if (!path) return
  if (path[0] === '') {
    // Most of the time the `.foo` property results in the first element being empty, so replace it with the idPrefix
    path[0] = 'root'
  } else {
    // Otherwise insert the idPrefix into the first location using unshift
    path.unshift(idPrefix)
  }

  //
  const elementId = path.join(idSeparator)
  // let field = formElement.elements[elementId]
  let field = formElement.querySelector(`#${elementId}`) as HTMLElement

  if (!field) {
    // if not an exact match, try finding an input starting with the element id (like radio buttons or checkboxes)
    // field = this.formElement.current.querySelector(`input[id^="${elementId}"`)
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

export const customFocusError = (wrapperRef: MutableRefObject<null>) => (error: RJSFValidationError) => {
  if (!wrapperRef.current) return

  const rjsForm = wrapperRef.current as Form

  focusOnError(rjsForm.formElement.current, error)
}
