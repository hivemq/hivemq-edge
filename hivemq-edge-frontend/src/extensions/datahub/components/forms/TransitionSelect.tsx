import type { FocusEvent } from 'react'
import { useCallback, useMemo } from 'react'
import type { WidgetProps } from '@rjsf/utils'
import { labelValue } from '@rjsf/utils'
import { FormControl, FormLabel, HStack, Text, VStack } from '@chakra-ui/react'
import type { OnChangeValue, SingleValueProps, OptionProps } from 'chakra-react-select'
import { Select, chakraComponents, createFilter } from 'chakra-react-select'

import type { FiniteStateMachine, FsmTransition } from '@datahub/types.ts'
import { FsmState } from '@datahub/types.ts'
import { useTranslation } from 'react-i18next'
import { getChakra } from '@/components/rjsf/utils/getChakra'

interface FsmTransitionWithId extends FsmTransition {
  id: string
  endStateType?: FsmState.Type
}

const SingleValue = (props: SingleValueProps<FsmTransitionWithId>) => {
  return (
    <chakraComponents.SingleValue {...props}>
      <Text>
        {props.data.event} (<Text as="span">{props.data.fromState}</Text> - <Text as="span">{props.data.toState}</Text>)
      </Text>
    </chakraComponents.SingleValue>
  )
}

const Option = (props: OptionProps<FsmTransitionWithId>) => {
  const { t } = useTranslation('datahub')
  const { isSelected, ...rest } = props
  const [selectedTransition] = props.getValue()

  // @ts-ignore
  const { __isNew__ } = props.data
  if (__isNew__) {
    return <chakraComponents.Option {...props}>{props.children}</chakraComponents.Option>
  }
  const isTerminal =
    props.data.endStateType === FsmState.Type.FAILED || props.data.endStateType === FsmState.Type.SUCCESS

  return (
    <chakraComponents.Option {...rest} isSelected={selectedTransition && selectedTransition.id === props.data.id}>
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
            {isTerminal && ` [${props.data.endStateType}]`}
          </Text>
        </VStack>
      </HStack>
    </chakraComponents.Option>
  )
}

export const TransitionSelect = (props: WidgetProps) => {
  const chakraProps = getChakra({ uiSchema: props.uiSchema })

  const onChange = useCallback<(newValue: OnChangeValue<FsmTransitionWithId, false>) => void>(
    (newValue) => {
      props.onChange(newValue?.id || undefined)
    },
    [props]
  )
  const onBlur = ({ target: { value } }: FocusEvent<HTMLInputElement>) => props.onBlur(props.id, value)
  const onFocus = ({ target: { value } }: FocusEvent<HTMLInputElement>) => props.onFocus(props.id, value)

  const options = useMemo(() => {
    const metadata = props.options.metadata as FiniteStateMachine | null
    if (!metadata) return []

    const states = metadata.states
    const opts = metadata.transitions.map<FsmTransitionWithId>((transition) => {
      const endState = states.find((state) => state.name === transition.toState)

      return {
        ...transition,
        id: `${transition.event}-${transition.fromState}-${transition.toState}-${endState?.type}`,
        endStateType: endState?.type,
      }
    })
    opts.push({
      id: 'Event.OnAny-Any.*-Any.*',
      event: 'Event.OnAny',
      toState: 'Any.*',
      fromState: 'Any.*',
      description: 'Matches every available event',
    })
    return opts
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

      <Select<FsmTransitionWithId>
        isClearable
        // isLoading={isLoading}
        // isInvalid={isError}
        instanceId={props.id}
        id={`${props.id}-container`}
        inputId={props.id}
        isRequired={props.required}
        options={options}
        value={options.find((e) => e.id === props.value)}
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
