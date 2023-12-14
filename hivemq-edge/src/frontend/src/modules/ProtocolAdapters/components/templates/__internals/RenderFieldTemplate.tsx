import { Box, FormControl, FormHelperText } from '@chakra-ui/react'
import { FieldTemplateProps } from '@rjsf/utils'
import { FC } from 'react'

export const RenderFieldTemplate: FC<FieldTemplateProps> = ({
  required,
  rawErrors,
  children,
  errors,
  displayLabel,
  rawDescription,
  description,
  help,
  label,
}) => {
  if (!displayLabel && !rawDescription && !label) {
    // This is assuming that no label/description means a mainly structuring element
    return <Box>{children}</Box>
  }

  return (
    <FormControl variant={'hivemq'} isRequired={required} isInvalid={rawErrors && rawErrors.length > 0}>
      {children}
      {rawErrors && rawErrors.length > 0 ? (
        errors
      ) : (
        <FormHelperText>{displayLabel && rawDescription ? description : null}</FormHelperText>
      )}
      {help}
    </FormControl>
  )
}
