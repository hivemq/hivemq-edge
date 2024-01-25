import { FC, useCallback, useState } from 'react'
import {
  CreatableSelect,
  OptionProps,
  SingleValueProps,
  chakraComponents,
  ActionMeta,
  OnChangeValue,
} from 'chakra-react-select'
import { FunctionSpecs } from '@/extensions/datahub/types.ts'
import { WidgetProps } from '@rjsf/utils'
import { HStack, VStack, Text } from '@chakra-ui/react'

const SingleValue = (props: SingleValueProps<FunctionSpecs>) => {
  return (
    <chakraComponents.SingleValue {...props}>
      <Text>{props.data.functionId}</Text>
    </chakraComponents.SingleValue>
  )
}

const Option = (props: OptionProps<FunctionSpecs>) => {
  const { isSelected, ...rest } = props
  const [dd] = props.getValue()

  // @ts-ignore
  const { __isNew__ } = props.data
  if (__isNew__) {
    return <chakraComponents.Option {...props}>{props.children}</chakraComponents.Option>
  }

  return (
    <chakraComponents.Option {...rest} isSelected={dd && dd.functionId === props.data.functionId}>
      <VStack w={'100%'} alignItems={'stretch'} gap={0}>
        <HStack>
          <Text as="b" flex={1}>
            {props.data.functionId}
          </Text>
          <Text fontSize={'sm'}>{props.data.isTerminal ? '[Terminal]' : ''}</Text>
        </HStack>
        <Text fontSize={'sm'}>{props.data.schema?.description}</Text>
      </VStack>
    </chakraComponents.Option>
  )
}

const getValue = (props: WidgetProps) => {
  const options = props.options.enumOptions
  const defs = props.registry.rootSchema.definitions

  if (!defs || !options) return undefined

  const deg = defs[props.value]
  if (!deg) return undefined

  return {
    schema: deg,
    functionId: props.value,
    // @ts-ignore
    ...deg.metadata,
  }
}

const getFunctions = (props: WidgetProps) => {
  const options = props.options.enumOptions
  const defs = props.registry.rootSchema.definitions

  if (!defs || !options) return []

  const ret: FunctionSpecs[] = options.map((e) => {
    const deg = defs[e.value]
    // const {} = deg
    if (!deg)
      return {
        functionId: 'unknown',
      }

    return {
      schema: deg,
      functionId: e.label,
      // @ts-ignore
      ...deg.metadata,
    }
  })

  return ret || []
}

const FunctionCreatableSelect: FC<WidgetProps> = (props) => {
  const [options] = useState<FunctionSpecs[]>(getFunctions(props))

  const onCreatableSelectChange = useCallback<
    (newValue: OnChangeValue<FunctionSpecs, false>, actionMeta: ActionMeta<FunctionSpecs>) => void
  >(
    (newValue) => {
      console.log('changed', newValue)
      if (newValue) props.onChange(newValue.functionId)
    },
    [props]
  )

  return (
    <>
      <CreatableSelect<FunctionSpecs, false>
        size="md"
        options={options}
        value={getValue(props)}
        onChange={onCreatableSelectChange}
        // onCreateOption={handleCreate}
        // formatCreateLabel={(inputValue) => {
        //   if (inputValue.startsWith('fn:')) {
        //     return `Create a new JS function: ${inputValue}`
        //   }
        //   return `Create a new JS function fn:${inputValue}`
        // }}
        components={{
          Option,
          SingleValue,
        }}
      />
    </>
  )
}

export default FunctionCreatableSelect
