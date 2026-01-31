import type { FocusEvent } from 'react'
import { useCallback, useMemo } from 'react'
import type { WidgetProps } from '@rjsf/utils'
import { labelValue } from '@rjsf/utils'
import { Badge, FormControl, FormLabel, HStack, Text, VStack } from '@chakra-ui/react'
import type { OnChangeValue, SingleValueProps, OptionProps } from 'chakra-react-select'
import { Select, chakraComponents, createFilter } from 'chakra-react-select'

import type { FiniteStateMachine, FsmTransition } from '@datahub/types.ts'
import { FsmState } from '@datahub/types.ts'
import { useTranslation } from 'react-i18next'
import { getChakra } from '@/components/rjsf/utils/getChakra'

interface FsmTransitionWithId extends FsmTransition {
  id: string
  endStateType?: FsmState.Type
  fromStateType?: FsmState.Type
  guards?: string
}

const SingleValue = (props: SingleValueProps<FsmTransitionWithId>) => {
  const displayName = props.data.guards ? `${props.data.event} + ${props.data.guards}` : props.data.event

  return (
    <chakraComponents.SingleValue {...props}>
      <HStack spacing={2}>
        <Text as="b">{displayName}</Text>
        <Text>
          (<Text as="span">{props.data.fromState}</Text> → <Text as="span">{props.data.toState}</Text>)
        </Text>
      </HStack>
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

  const displayName = props.data.guards ? `${props.data.event} + ${props.data.guards}` : props.data.event

  const getStateColorScheme = (stateType?: FsmState.Type): string => {
    switch (stateType) {
      case FsmState.Type.INITIAL:
        return 'blue'
      case FsmState.Type.INTERMEDIATE:
        return 'gray'
      case FsmState.Type.SUCCESS:
        return 'green'
      case FsmState.Type.FAILED:
        return 'red'
      default:
        return 'gray'
    }
  }

  return (
    <chakraComponents.Option {...rest} isSelected={selectedTransition && selectedTransition.id === props.data.id}>
      <HStack width="100%" justifyContent="space-between" gap={3}>
        <VStack align="flex-start" spacing={1} flex={1}>
          <Text as="b">{displayName}</Text>
          <Text fontSize="sm">{props.data.description}</Text>
        </VStack>

        <VStack align="flex-end" spacing={1} fontSize="xs" whiteSpace="nowrap">
          <HStack spacing={1}>
            <Text>{t('workspace.transition.select.fromState')}</Text>
            <Badge colorScheme={getStateColorScheme(props.data.fromStateType)} fontSize="xs">
              {props.data.fromState}
            </Badge>
          </HStack>

          <HStack spacing={1}>
            <Text>{t('workspace.transition.select.toState')}</Text>
            <Badge colorScheme={getStateColorScheme(props.data.endStateType)} fontSize="xs">
              {props.data.toState}
            </Badge>
          </HStack>
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
      const fromState = states.find((state) => state.name === transition.fromState)

      return {
        ...transition,
        id: `${transition.event}-${transition.fromState}-${transition.toState}-${endState?.type}`,
        endStateType: endState?.type,
        fromStateType: fromState?.type,
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
