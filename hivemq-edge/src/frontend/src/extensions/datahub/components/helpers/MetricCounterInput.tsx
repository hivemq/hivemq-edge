import { ChangeEvent, FocusEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { labelValue, WidgetProps } from '@rjsf/utils'
import { getChakra } from '@rjsf/chakra-ui/lib/utils'
import { FormControl, FormLabel, Input, InputGroup, InputLeftAddon } from '@chakra-ui/react'

export const MetricCounterInput = (props: WidgetProps) => {
  const { t } = useTranslation('datahub')
  const chakraProps = getChakra({ uiSchema: props.uiSchema })

  const _onChange = ({ target: { value } }: ChangeEvent<HTMLInputElement>) =>
    props.onChange(value === '' ? props.options.emptyValue : value)
  const _onBlur = ({ target: { value } }: FocusEvent<HTMLInputElement>) => props.onBlur(props.id, value)
  const _onFocus = ({ target: { value } }: FocusEvent<HTMLInputElement>) => props.onFocus(props.id, value)

  return (
    <FormControl
      mb={1}
      {...chakraProps}
      isDisabled={props.disabled || props.readonly}
      isRequired={props.required}
      isReadOnly={props.readonly}
      isInvalid={props.rawErrors && props.rawErrors.length > 0}
    >
      {labelValue(<FormLabel htmlFor={props.id}>{props.label}</FormLabel>, props.hideLabel || !props.label)}

      <InputGroup>
        <InputLeftAddon>com.hivemq.data-hub.custom.counters.</InputLeftAddon>
        <Input
          id={props.id}
          isRequired={props.required}
          placeholder={t('workspace.function.metricName.placeholder') as string}
          value={props.value}
          onBlur={_onBlur}
          onFocus={_onFocus}
          onChange={_onChange}
        />
      </InputGroup>
    </FormControl>
  )
}
