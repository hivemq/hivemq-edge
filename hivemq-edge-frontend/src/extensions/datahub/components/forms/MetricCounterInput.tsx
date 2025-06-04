import type { ChangeEvent, FocusEvent } from 'react'
import { useTranslation } from 'react-i18next'
import type { WidgetProps } from '@rjsf/utils'
import { labelValue } from '@rjsf/utils'
import { FormControl, FormLabel, Input, InputGroup, InputLeftAddon } from '@chakra-ui/react'
import { getChakra } from '@/components/rjsf/utils/getChakra'

const PrefixInput = (prefix: string, placeholder: string, props: WidgetProps) => {
  const { t } = useTranslation('datahub')
  const chakraProps = getChakra({ uiSchema: props.uiSchema })

  const onChange = ({ target: { value } }: ChangeEvent<HTMLInputElement>) =>
    props.onChange(value === '' ? props.options.emptyValue : value)
  const onBlur = ({ target: { value } }: FocusEvent<HTMLInputElement>) => props.onBlur(props.id, value)
  const onFocus = ({ target: { value } }: FocusEvent<HTMLInputElement>) => props.onFocus(props.id, value)

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

      <InputGroup>
        <InputLeftAddon>{prefix}</InputLeftAddon>
        <Input
          id={props.id}
          isRequired={props.required}
          placeholder={t(placeholder)}
          value={props.value}
          onBlur={onBlur}
          onFocus={onFocus}
          onChange={onChange}
        />
      </InputGroup>
    </FormControl>
  )
}

export const MetricCounterInput = (props: WidgetProps) =>
  PrefixInput('com.hivemq.com.data-hub.custom.counters.', 'workspace.function.metricName.placeholder', props)

export const JsFunctionInput = (props: WidgetProps) =>
  PrefixInput('fn:', 'workspace.function.transform.placeholder', props)
