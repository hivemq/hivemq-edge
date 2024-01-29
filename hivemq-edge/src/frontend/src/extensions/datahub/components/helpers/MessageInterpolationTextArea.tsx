import { ChangeEvent, FC, FocusEvent, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { labelValue, WidgetProps } from '@rjsf/utils'
import { getChakra } from '@rjsf/chakra-ui/lib/utils'
import { Button, ButtonGroup, FormControl, FormLabel, Textarea, VStack } from '@chakra-ui/react'

export const MessageInterpolationTextArea = (props: WidgetProps) => {
  const { t } = useTranslation('datahub')
  const chakraProps = getChakra({ uiSchema: props.uiSchema })

  const [currentSelection, setCurrentSelection] = useState<[number, number] | undefined>(undefined)

  const onChange = ({ target: { value, selectionStart, selectionEnd } }: ChangeEvent<HTMLTextAreaElement>) => {
    props.onChange(value === '' ? props.options.emptyValue : value)
    setCurrentSelection([selectionStart, selectionEnd])
  }
  const onBlur = ({ target: { value, selectionStart, selectionEnd } }: FocusEvent<HTMLTextAreaElement>) => {
    // TODO[NVL] the state change impacts on the blur
    setCurrentSelection([selectionStart, selectionEnd])
    props.onBlur(props.id, value)
  }
  const onFocus = ({ target: { value } }: FocusEvent<HTMLTextAreaElement>) => {
    props.onFocus(props.id, value)
  }

  const Interpolation: FC<{ text: string; icon: string }> = ({ text, icon }) => (
    <Button
      onClick={() => {
        if (currentSelection) {
          const state = props.value

          props.onChange(state.slice(0, currentSelection[0]) + ` ${icon} ` + state.slice(currentSelection[0]))
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
          onBlur={onBlur}
          onFocus={onFocus}
          onChange={onChange}
        />
      </VStack>
    </FormControl>
  )
}
