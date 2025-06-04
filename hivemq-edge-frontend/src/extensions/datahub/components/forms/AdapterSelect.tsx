import type { FocusEvent } from 'react'
import { useCallback, useMemo } from 'react'
import type { WidgetProps } from '@rjsf/utils'
import { labelValue } from '@rjsf/utils'
import { getChakra } from '@/components/rjsf/utils/getChakra'
import { FormControl, FormLabel, HStack, Text, VStack } from '@chakra-ui/react'
import type { OnChangeValue, SingleValueProps, OptionProps } from 'chakra-react-select'
import { Select, chakraComponents } from 'chakra-react-select'

import type { Adapter } from '@/api/__generated__'
import { useListProtocolAdapters } from '@/api/hooks/useProtocolAdapters/useListProtocolAdapters.ts'

const SingleValue = (props: SingleValueProps<Adapter>) => {
  return (
    <chakraComponents.SingleValue {...props}>
      <Text>{props.data.id}</Text>
    </chakraComponents.SingleValue>
  )
}

const Option = (props: OptionProps<Adapter>) => {
  const { isSelected, ...rest } = props
  const [selectedAdapter] = props.getValue()

  // @ts-ignore
  const { __isNew__ } = props.data
  if (__isNew__) {
    return <chakraComponents.Option {...props}>{props.children}</chakraComponents.Option>
  }

  return (
    <chakraComponents.Option {...rest} isSelected={selectedAdapter && selectedAdapter.id === props.data.id}>
      <VStack w="100%" alignItems="stretch" gap={0}>
        <HStack>
          <Text as="b" flex={1}>
            {props.data.id}
          </Text>
          <HStack>
            <Text fontSize="sm">{props.data.type}</Text>
          </HStack>
        </HStack>
        <Text fontSize="sm">{props.data.type}</Text>
      </VStack>
    </chakraComponents.Option>
  )
}

export const AdapterSelect = (props: WidgetProps) => {
  // TODO[19017] This is one of the components that break boundary with the DataHub "extension". Need a better way
  const { data: adapters, isLoading, isError } = useListProtocolAdapters()

  const chakraProps = getChakra({ uiSchema: props.uiSchema })

  const onChange = useCallback<(newValue: OnChangeValue<Adapter, false>) => void>(
    (newValue) => {
      props.onChange(newValue?.id || undefined)
    },
    [props]
  )
  const onBlur = ({ target: { value } }: FocusEvent<HTMLInputElement>) => props.onBlur(props.id, value)
  const onFocus = ({ target: { value } }: FocusEvent<HTMLInputElement>) => props.onFocus(props.id, value)

  const options = useMemo(() => {
    if (!adapters) return []
    return adapters
  }, [adapters])

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

      <Select<Adapter>
        isClearable
        isLoading={isLoading}
        isInvalid={isError}
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
      />
    </FormControl>
  )
}
