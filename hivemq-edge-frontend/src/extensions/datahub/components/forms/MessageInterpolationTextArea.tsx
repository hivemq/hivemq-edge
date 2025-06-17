import { useTranslation } from 'react-i18next'
import type { WidgetProps } from '@rjsf/utils'
import { labelValue } from '@rjsf/utils'
import { FormControl, FormLabel } from '@chakra-ui/react'

import { getChakra } from '@/components/rjsf/utils/getChakra'
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
        readonly={props.readonly}
        placeholder={t('workspace.function.message.placeholder')}
        value={props.value}
        onChange={onChange}
        isInvalid={(props.required && !props.value) || (props.rawErrors && props.rawErrors.length > 0)}
      />
    </FormControl>
  )
}
