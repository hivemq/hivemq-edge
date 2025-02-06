import { memo } from 'react'
import { IconButton } from '@chakra-ui/react'
type ChakraIconButtonProps = React.ComponentProps<typeof IconButton>
import type { FormContextType, IconButtonProps, RJSFSchema, StrictRJSFSchema } from '@rjsf/utils'

/**
 * TODO[rjsf-team/react-jsonschema-form/issues/3839] Bug with disabled in buttons
 * Replaces
 *   // const {
 *   //   ButtonTemplates: { AddButton },
 *   // } = registry.templates
 * @see https://github.com/rjsf-team/react-jsonschema-form/issues/3839
 * @see https://github.com/rjsf-team/react-jsonschema-form/blob/main/packages/chakra-ui/src/AddButton/AddButton.tsx
 */

function ChakraIconButton<T = never, S extends StrictRJSFSchema = RJSFSchema, F extends FormContextType = never>(
  props: IconButtonProps<T, S, F>
) {
  const { icon, title, disabled, iconType, uiSchema, registry, ...otherProps } = props
  return (
    <IconButton
      aria-label={title || ''}
      {...otherProps}
      isDisabled={props.disabled}
      icon={icon as ChakraIconButtonProps['icon']}
    />
  )
}

ChakraIconButton.displayName = 'ChakraIconButton'

export default memo(ChakraIconButton) as typeof ChakraIconButton
