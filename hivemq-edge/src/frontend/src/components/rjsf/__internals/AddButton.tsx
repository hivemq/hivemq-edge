import type { FormContextType, IconButtonProps, RJSFSchema, StrictRJSFSchema } from '@rjsf/utils'
import { TranslatableString } from '@rjsf/utils'
import { Button } from '@chakra-ui/react'
import { AddIcon } from '@chakra-ui/icons'

/**
 * TODO[rjsf-team/react-jsonschema-form/issues/3839] Bug with disabled in buttons
 * Replaces
 *   // const {
 *   //   ButtonTemplates: { AddButton },
 *   // } = registry.templates
 * @see https://github.com/rjsf-team/react-jsonschema-form/issues/3839
 * @see https://github.com/rjsf-team/react-jsonschema-form/blob/main/packages/chakra-ui/src/AddButton/AddButton.tsx
 */
export default function AddButton<
  T = never,
  S extends StrictRJSFSchema = RJSFSchema,
  F extends FormContextType = never
>({ uiSchema, registry, ...props }: IconButtonProps<T, S, F>) {
  const { translateString } = registry
  const { disabled, 'aria-label': ariaLabel, ...rest } = props
  return (
    <Button leftIcon={<AddIcon />} {...rest} isDisabled={props.disabled}>
      {ariaLabel || translateString(TranslatableString.AddItemButton)}
    </Button>
  )
}
