import { FC } from 'react'
import { FieldTemplateProps } from '@rjsf/utils'
import { FormControl, FormErrorMessage, FormHelperText } from '@chakra-ui/react'

export const RenderFieldTemplate: FC<FieldTemplateProps> = ({
  required,
  rawErrors,
  children,
  errors,
  displayLabel,
  rawDescription,
  description,
  help,
}) => {
  return (
    <FormControl isRequired={required} isInvalid={rawErrors && rawErrors.length > 0}>
      {children}
      {rawErrors && rawErrors.length > 0 ? (
        <FormErrorMessage>{errors}</FormErrorMessage>
      ) : (
        <FormHelperText>{displayLabel && rawDescription ? description : null}</FormHelperText>
      )}
      {help}
    </FormControl>
  )
}
