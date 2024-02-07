import { FocusEvent, useCallback, useMemo } from 'react'
import { labelValue, WidgetProps } from '@rjsf/utils'
import { getChakra } from '@rjsf/chakra-ui/lib/utils'
import { FormControl, FormLabel, HStack, Text, VStack } from '@chakra-ui/react'
import { Select, OnChangeValue, SingleValueProps, chakraComponents, OptionProps } from 'chakra-react-select'

import { FiniteStateMachine, FsmTransition } from '@datahub/types.ts'
import { useTranslation } from 'react-i18next'

const SingleValue = (props: SingleValueProps<FsmTransition>) => {
  return (
    <chakraComponents.SingleValue {...props}>
      <Text>{props.data.event}</Text>
    </chakraComponents.SingleValue>
  )
}

const Option = (props: OptionProps<FsmTransition>) => {
  const { t } = useTranslation('datahub')
  const { isSelected, ...rest } = props
  const [selectedTransition] = props.getValue()

  // @ts-ignore
  const { __isNew__ } = props.data
  if (__isNew__) {
    return <chakraComponents.Option {...props}>{props.children}</chakraComponents.Option>
  }

  return (
    <chakraComponents.Option {...rest} isSelected={selectedTransition && selectedTransition.event === props.data.event}>
      <HStack w="100%" justifyContent="space-between" gap={3}>
        <VStack alignItems="flex-start">
          <Text as="b" flex={1}>
            {props.data.event}
          </Text>
          <HStack>
            <Text fontSize="sm">{props.data.description}</Text>
          </HStack>
        </VStack>
        <VStack alignItems="flex-end">
          <Text fontSize="sm" whiteSpace="nowrap">
            {t('workspace.transition.select.fromState', { state: props.data.fromState })}
          </Text>
          <Text fontSize="sm" whiteSpace="nowrap">
            {t('workspace.transition.select.toState', { state: props.data.toState })}
          </Text>
        </VStack>
      </HStack>
    </chakraComponents.Option>
  )
}

export const TransitionSelect = (props: WidgetProps) => {
  const chakraProps = getChakra({ uiSchema: props.uiSchema })

  const onChange = useCallback<(newValue: OnChangeValue<FsmTransition, false>) => void>(
    (newValue) => {
      props.onChange(newValue?.event || undefined)
    },
    [props]
  )
  const onBlur = ({ target: { value } }: FocusEvent<HTMLInputElement>) => props.onBlur(props.id, value)
  const onFocus = ({ target: { value } }: FocusEvent<HTMLInputElement>) => props.onFocus(props.id, value)

  const options = useMemo(() => {
    const metadata = props.options.metadata as FiniteStateMachine | null
    if (!metadata) return []
    return metadata.transitions
  }, [props.options])

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

      <Select<FsmTransition>
        isClearable
        // isLoading={isLoading}
        // isInvalid={isError}
        inputId={props.id}
        isRequired={props.required}
        options={options}
        value={options.find((e) => e.event === props.value)}
        components={{
          Option,
          SingleValue,
        }}
        onChange={onChange}
        onBlur={onBlur}
        onFocus={onFocus}
        filterOption={createFilter({
          stringify: (option) => {
            return `${option.data.event}${option.data.fromState}${option.data.toState}`
          },
        })}
      />
    </FormControl>
  )
}
