import type { FC } from 'react'
import type { FieldProps } from '@rjsf/utils'
import { getUiOptions, labelValue } from '@rjsf/utils'
import type { RJSFSchema } from '@rjsf/utils'
import { getChakra } from '@/components/rjsf/utils/getChakra'
import type { AlertStatus } from '@chakra-ui/react'
import { Alert, AlertDescription, AlertIcon, FormControl, FormLabel } from '@chakra-ui/react'
import type { AdapterContext } from '@/modules/ProtocolAdapters/types.ts'

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
