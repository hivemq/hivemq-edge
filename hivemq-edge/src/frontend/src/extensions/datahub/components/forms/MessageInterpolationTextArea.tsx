import { useTranslation } from 'react-i18next'
import { labelValue, WidgetProps } from '@rjsf/utils'
import { getChakra } from '@rjsf/chakra-ui/lib/utils'
import { FormControl, FormLabel } from '@chakra-ui/react'

import { Editor } from '@datahub/components/interpolation/Editor.tsx'

export const MessageInterpolationTextArea = (props: WidgetProps) => {
  const { t } = useTranslation('datahub')
  const chakraProps = getChakra({ uiSchema: props.uiSchema })

  const onChange = (value: string) => {
    props.onChange(value === '' ? props.options.emptyValue : value)
  }

  return (
    <FormControl
      mb={1}
      {...chakraProps}
      isDisabled={props.disabled || props.readonly}
      isRequired={props.required}
      isReadOnly={props.readonly}
      isInvalid={props.rawErrors && props.rawErrors.length > 0}
    >
      {labelValue(
        <FormLabel htmlFor={props.id} id={`${props.id}-label`}>
          {props.label}
        </FormLabel>,
        props.hideLabel || !props.label
      )}

      <Editor
        id={props.id}
        labelId={`${props.id}-label`}
        isRequired={props.required}
        placeholder={t('workspace.function.message.placeholder') as string}
        value={props.value}
        onChange={onChange}
        isInvalid={props.rawErrors && props.rawErrors.length > 0}
      />
    </FormControl>
  )
}
