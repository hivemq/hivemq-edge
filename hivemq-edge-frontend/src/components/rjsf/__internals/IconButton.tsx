import type { FormContextType, IconButtonProps, RJSFSchema, StrictRJSFSchema } from '@rjsf/utils'
import { TranslatableString } from '@rjsf/utils'
import { ArrowUpIcon, ArrowDownIcon, CopyIcon, DeleteIcon } from '@chakra-ui/icons'

import ChakraIconButton from '@/components/rjsf/__internals/ChakraIconButton.tsx'

/**
 * TODO[rjsf-team/react-jsonschema-form/issues/3839] Bug with disabled in buttons
 * Replaces
 *   // const { CopyButton, MoveDownButton, MoveUpButton, RemoveButton } = registry.templates.ButtonTemplates
 * @see https://github.com/rjsf-team/react-jsonschema-form/issues/3839
 * @see https://github.com/rjsf-team/react-jsonschema-form/blob/main/packages/chakra-ui/src/AddButton/AddButton.tsx
 */

export function CopyButton<T = never, S extends StrictRJSFSchema = RJSFSchema, F extends FormContextType = never>(
  props: IconButtonProps<T, S, F>
) {
  const {
    registry: { translateString },
  } = props
  return (
    <ChakraIconButton<T, S, F> title={translateString(TranslatableString.CopyButton)} {...props} icon={<CopyIcon />} />
  )
}

export function MoveDownButton<T = never, S extends StrictRJSFSchema = RJSFSchema, F extends FormContextType = never>(
  props: IconButtonProps<T, S, F>
) {
  const {
    registry: { translateString },
  } = props
  return (
    <ChakraIconButton<T, S, F>
      title={translateString(TranslatableString.MoveDownButton)}
      {...props}
      icon={<ArrowDownIcon />}
    />
  )
}

export function MoveUpButton<T = never, S extends StrictRJSFSchema = RJSFSchema, F extends FormContextType = never>(
  props: IconButtonProps<T, S, F>
) {
  const {
    registry: { translateString },
  } = props
  return (
    <ChakraIconButton<T, S, F>
      title={translateString(TranslatableString.MoveUpButton)}
      {...props}
      icon={<ArrowUpIcon />}
    />
  )
}

export function RemoveButton<T = never, S extends StrictRJSFSchema = RJSFSchema, F extends FormContextType = never>(
  props: IconButtonProps<T, S, F>
) {
  const {
    registry: { translateString },
  } = props
  return (
    <ChakraIconButton<T, S, F>
      title={translateString(TranslatableString.RemoveButton)}
      {...props}
      icon={<DeleteIcon />}
    />
  )
}
