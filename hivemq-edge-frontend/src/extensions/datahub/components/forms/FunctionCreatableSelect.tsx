import type { FC } from 'react'
import { useCallback } from 'react'
import type { OptionProps, SingleValueProps, ActionMeta, OnChangeValue } from 'chakra-react-select'
import { chakraComponents, Select } from 'chakra-react-select'
import type { WidgetProps, RJSFSchema } from '@rjsf/utils'
import { labelValue } from '@rjsf/utils'
import { HStack, VStack, Text, FormLabel, FormControl } from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'

import { getChakra } from '@/components/rjsf/utils/getChakra'

import type { FunctionSpecs, ReactFlowSchemaFormContext } from '@datahub/types.ts'

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
  const [selectedOption] = props.getValue()

  // @ts-ignore
  const { __isNew__ } = props.data
  if (__isNew__) {
    return <chakraComponents.Option {...props}>{props.children}</chakraComponents.Option>
  }

  const { isTerminal, isDataOnly } = props.data.metadata || {}

  return (
    <chakraComponents.Option
      {...rest}
      isSelected={selectedOption && selectedOption.functionId === props.data.functionId}
    >
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
  const rootSchemaDefinitions = props.registry.rootSchema.definitions

  if (!rootSchemaDefinitions || !options) return undefined

  const schemaDefinition = rootSchemaDefinitions[props.value]
  if (!schemaDefinition) return undefined

  return {
    schema: schemaDefinition,
    functionId: props.value,
    // @ts-ignore
    ...schemaDefinition.metadata,
  }
}

const filterOption = (option: { data: FunctionSpecs }, inputValue: string) => {
  const { functionId, schema } = option.data
  const description = schema?.description || ''
  const title = schema?.title || ''
  const search = inputValue.toLowerCase()
  return (
    functionId?.toLowerCase().includes(search) ||
    description.toLowerCase().includes(search) ||
    title.toLowerCase().includes(search)
  )
}

const FunctionCreatableSelect: FC<WidgetProps<unknown, RJSFSchema, ReactFlowSchemaFormContext>> = (props) => {
  const chakraProps = getChakra({ uiSchema: props.uiSchema })
  const functions = props.formContext?.functions || []

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
    <FormControl
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

      <Select<FunctionSpecs, false>
        inputId={props.id}
        instanceId="functions"
        size="md"
        options={functions}
        value={value}
        onChange={onCreatableSelectChange}
        components={{
          Option,
          SingleValue,
        }}
        filterOption={filterOption}
      />
    </FormControl>
  )
}

export default FunctionCreatableSelect
