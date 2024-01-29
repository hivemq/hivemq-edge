import { ChangeEvent, FC, FocusEvent, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { labelValue, WidgetProps } from '@rjsf/utils'
import { getChakra } from '@rjsf/chakra-ui/lib/utils'
import { Button, ButtonGroup, FormControl, FormLabel, Textarea, VStack } from '@chakra-ui/react'

export const MessageInterpolationTextArea = (props: WidgetProps) => {
  const { t } = useTranslation('datahub')
  const chakraProps = getChakra({ uiSchema: props.uiSchema })

  const [cur, setCur] = useState<[number, number] | undefined>(undefined)

  const _onChange = ({ target: { value, selectionStart, selectionEnd } }: ChangeEvent<HTMLTextAreaElement>) => {
    props.onChange(value === '' ? props.options.emptyValue : value)
    setCur([selectionStart, selectionEnd])
  }
  const _onBlur = ({ target: { value, selectionStart, selectionEnd } }: FocusEvent<HTMLTextAreaElement>) => {
    // TODO[NVL] the state change impacts on the blur
    setCur([selectionStart, selectionEnd])
    props.onBlur(props.id, value)
  }
  const _onFocus = ({ target: { value } }: FocusEvent<HTMLTextAreaElement>) => {
    props.onFocus(props.id, value)
  }

  const Interpolation: FC<{ text: string; icon: string }> = ({ text, icon }) => (
    <Button
      onClick={() => {
        if (cur) {
          const state = props.value

          props.onChange(state.slice(0, cur[0]) + ` ${icon} ` + state.slice(cur[0]))
        }
      }}
    >
      {icon} {text}
    </Button>
  )

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
      <VStack alignItems="flex-start">
        <ButtonGroup size="xs" variant="outline" fontFamily="monospace" flexWrap="wrap">
          <Interpolation text="clientId" icon="#️⃣" />
          <Interpolation text="policyId" icon="*️⃣" />
          <ButtonGroup isAttached size="xs">
            <Interpolation text="fromState" icon="📗" />
            <Interpolation text="toState" icon="📕" />
          </ButtonGroup>
          <Interpolation text="validationResult" icon="🧾" />
          <Interpolation text="triggerEvent" icon="☑️" />
          <Interpolation text="timestamp" icon="⏲️" />
        </ButtonGroup>
        <Textarea
          id={props.id}
          isRequired={props.required}
          placeholder={t('workspace.function.metricName.placeholder') as string}
          value={props.value}
          onBlur={_onBlur}
          onFocus={_onFocus}
          onChange={_onChange}
        />
      </VStack>
    </FormControl>
  )
}
