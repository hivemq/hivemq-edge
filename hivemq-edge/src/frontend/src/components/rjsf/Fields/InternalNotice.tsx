import { FC } from 'react'
import { FieldProps, getUiOptions, labelValue } from '@rjsf/utils'
import { RJSFSchema } from '@rjsf/utils/src/types.ts'
import { getChakra } from '@rjsf/chakra-ui/lib/utils'
import { Alert, AlertDescription, AlertIcon, AlertStatus, FormControl, FormLabel } from '@chakra-ui/react'
import { AdapterContext } from '@/modules/ProtocolAdapters/types.ts'

export const InternalNotice: FC<FieldProps<unknown, RJSFSchema, AdapterContext>> = (props) => {
  const chakraProps = getChakra({ uiSchema: props.uiSchema })
  const { message, status } = getUiOptions(props.uiSchema)

  return (
    <FormControl {...chakraProps}>
      {labelValue(
        <FormLabel htmlFor={props.id} id={`${props.id}-label`}>
          {props.label}
        </FormLabel>,
        props.hideLabel || !props.label
      )}

      <Alert status={(status as AlertStatus) || 'info'}>
        <AlertIcon />
        {message && <AlertDescription maxWidth="sm">{message as string}</AlertDescription>}
      </Alert>
    </FormControl>
  )
}
