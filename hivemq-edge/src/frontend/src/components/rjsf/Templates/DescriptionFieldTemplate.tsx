import { DescriptionFieldProps, FormContextType, RJSFSchema, StrictRJSFSchema } from '@rjsf/utils'
import { GenericObjectType } from '@rjsf/utils/src/types.ts'
import { Text } from '@chakra-ui/react'

export const DescriptionFieldTemplate = <
  T = unknown,
  S extends StrictRJSFSchema = RJSFSchema,
  F extends FormContextType = GenericObjectType
>({
  description,
  id,
}: DescriptionFieldProps<T, S, F>) => {
  if (!description) {
    return null
  }

  // Override to fix bug with nested p
  if (typeof description === 'string') {
    return (
      <Text id={id} mt={2} mb={4}>
        {description}
      </Text>
    )
  }

  return <>{description}</>
}
