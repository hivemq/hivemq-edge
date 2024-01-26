import { FC, useCallback } from 'react'
import {
  CreatableSelect,
  OptionProps,
  SingleValueProps,
  chakraComponents,
  ActionMeta,
  OnChangeValue,
} from 'chakra-react-select'
import { WidgetProps } from '@rjsf/utils'
import { HStack, VStack, Text } from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'

import { FunctionSpecs } from '../../types.ts'
import useDataHubDraftStore from '../../hooks/useDataHubDraftStore.ts'

const SingleValue = (props: SingleValueProps<FunctionSpecs>) => {
  return (
    <chakraComponents.SingleValue {...props}>
      <Text>{props.data.functionId}</Text>
    </chakraComponents.SingleValue>
  )
}

const Option = (props: OptionProps<FunctionSpecs>) => {
  const { t } = useTranslation('datahub')
  const { isSelected, ...rest } = props
  const [dd] = props.getValue()

  // @ts-ignore
  const { __isNew__ } = props.data
  if (__isNew__) {
    return <chakraComponents.Option {...props}>{props.children}</chakraComponents.Option>
  }

  const { isTerminal, isDataOnly } = props.data.metadata || {}

  return (
    <chakraComponents.Option {...rest} isSelected={dd && dd.functionId === props.data.functionId}>
      <VStack w="100%" alignItems="stretch" gap={0}>
        <HStack>
          <Text as="b" flex={1}>
            {props.data.functionId}
          </Text>
          <HStack>
            {isTerminal && <Text fontSize="sm">{t('workspace.function.isTerminal')}</Text>}
            {isDataOnly && <Text fontSize="sm">{t('workspace.function.isDataOnly')}</Text>}
          </HStack>
        </HStack>
        <Text fontSize="sm">{props.data.schema?.description}</Text>
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

const FunctionCreatableSelect: FC<WidgetProps> = (props) => {
  const { functions } = useDataHubDraftStore()

  const onCreatableSelectChange = useCallback<
    (newValue: OnChangeValue<FunctionSpecs, false>, actionMeta: ActionMeta<FunctionSpecs>) => void
  >(
    (newValue) => {
      if (newValue) props.onChange(newValue.functionId)
    },
    [props]
  )

  const value = getValue(props)

  return (
    <>
      <CreatableSelect<FunctionSpecs, false>
        size="md"
        options={functions}
        value={value}
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
