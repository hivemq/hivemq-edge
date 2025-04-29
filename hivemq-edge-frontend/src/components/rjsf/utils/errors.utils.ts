import type { ErrorSchema, FieldErrors } from '@rjsf/utils'

export const hasNestedError = <T>(props: ErrorSchema<T> | undefined) => {
  if (!props) return false
  const { __errors, ...rest } = props
  for (const subs of Object.keys(rest)) {
    const hasError = hasNestedError((props as never)[subs])
    if (hasError) return true
  }

  const errors = (props as FieldErrors).__errors
  return errors ? errors.length > 0 : false
}
