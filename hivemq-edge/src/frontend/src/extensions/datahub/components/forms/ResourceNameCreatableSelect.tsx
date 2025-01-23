import { useCallback, useEffect, useMemo, useState } from 'react'
import type { ActionMeta, OnChangeValue, OptionProps, Options, SingleValueProps } from 'chakra-react-select'
import { chakraComponents, CreatableSelect, createFilter } from 'chakra-react-select'
import { getChakra } from '@rjsf/chakra-ui/lib/utils'
import type { WidgetProps } from '@rjsf/utils'
import { labelValue } from '@rjsf/utils'
import { FormControl, FormLabel, HStack, Text, VStack } from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'

import type { FunctionData, ResourceFamily, SchemaData } from '@datahub/types.ts'
import { DataHubNodeType, ResourceStatus } from '@datahub/types.ts'
import { useGetAllSchemas } from '@datahub/api/hooks/DataHubSchemasService/useGetAllSchemas.tsx'
import { useGetAllScripts } from '@datahub/api/hooks/DataHubScriptsService/useGetAllScripts.tsx'
import { getSchemaFamilies, getScriptFamilies } from '@datahub/designer/schema/SchemaNode.utils.ts'
import { getNodePayload } from '@datahub/utils/node.utils.ts'

const SingleValue = <T extends ResourceFamily>(props: SingleValueProps<T>) => {
  return (
    <chakraComponents.SingleValue {...props}>
      <Text>{props.data.name || props.data.label}</Text>
    </chakraComponents.SingleValue>
  )
}

const Option = <T extends ResourceFamily>(props: OptionProps<T>) => {
  const { t } = useTranslation('datahub')
  const { isSelected, ...rest } = props
  const [selectedOption] = props.getValue()

  // @ts-ignore
  const { __isNew__ } = props.data
  if (__isNew__) {
    return <chakraComponents.Option {...props}>{props.children}</chakraComponents.Option>
  }

  const [firstItem, ...all] = props.data.versions
  const lastItem = all.pop()

  return (
    <chakraComponents.Option {...rest} isSelected={selectedOption && selectedOption.name === props.data.name}>
      <VStack w="100%" alignItems="stretch" gap={0}>
        <HStack>
          <Text as="b" flex={1}>
            {props.data.name}
            {props.data.internalStatus && ` ${t('workspace.name.draft')}`}
          </Text>
          <HStack>
            <Text fontSize="sm">{props.data.type}</Text>
            {props.data.versions.length && (
              <Text fontSize="sm">{lastItem ? `[${firstItem}...${lastItem}]` : `[${firstItem}]`}</Text>
            )}
          </HStack>
        </HStack>
        <Text fontSize="sm">{props.data.description}</Text>
      </VStack>
    </chakraComponents.Option>
  )
}

const ResourceNameCreatableSelect = (
  props: WidgetProps,
  type: DataHubNodeType.SCHEMA | DataHubNodeType.FUNCTION,
  defaultOptions: Options<ResourceFamily>,
  createNewOption: (inputValue: string) => ResourceFamily,
  isLoading: boolean
) => {
  const { t } = useTranslation('datahub')
  const chakraProps = getChakra({ uiSchema: props.uiSchema })
  const [options, setOptions] = useState(defaultOptions)

  useEffect(() => {
    if (defaultOptions.length) {
      const options = [...defaultOptions]
      const { isDraft } = props.options
      if (isDraft)
        options.push({
          name: props.value,
          versions: [],
        })
      setOptions(options)
    }
  }, [defaultOptions, props.options, props.value])

  const onCreatableSelectChange = useCallback<
    (newValue: OnChangeValue<ResourceFamily, false>, actionMeta: ActionMeta<ResourceFamily>) => void
  >(
    (newValue, actionMeta) => {
      if (actionMeta.action === 'select-option' && newValue) {
        props.onChange(newValue.name)
        return
      }
      if (actionMeta.action === 'create-option' && newValue) {
        props.onChange(newValue.label)
        return
      }
    },
    [props]
  )

  const handleCreate = (inputValue: string) => {
    setOptions((prev) => [...(prev || []), createNewOption(inputValue)])
    props.onChange(inputValue)
  }

  const value = options?.find((e) => e.name === props.value)
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

      <CreatableSelect<ResourceFamily, false>
        inputId={props.id}
        options={options}
        value={value}
        isLoading={isLoading}
        onChange={onCreatableSelectChange}
        components={{
          Option,
          SingleValue,
        }}
        filterOption={createFilter({
          // search in the name and description
          stringify: (option) => {
            return `${option.data.name}${option.data.description}`
          },
        })}
        noOptionsMessage={() => t('workspace.name.noOption', { type })}
        formatCreateLabel={(inputValue) => t('workspace.name.createOption', { type, inputValue })}
        onCreateOption={handleCreate}
      />
    </FormControl>
  )
}

const createNewSchemaOption = (inputValue: string) => {
  const schemaData = getNodePayload(DataHubNodeType.SCHEMA) as SchemaData
  const newValue: ResourceFamily = {
    name: inputValue,
    versions: [1],
    type: schemaData.type,
    internalStatus: ResourceStatus.DRAFT,
  }
  return newValue
}

const createNewScriptOption = (inputValue: string) => {
  const functionData = getNodePayload(DataHubNodeType.FUNCTION) as FunctionData
  const newValue: ResourceFamily = {
    name: inputValue,
    versions: [1],
    type: functionData.type,
  }
  return newValue
}

export const SchemaNameCreatableSelect = (props: WidgetProps) => {
  const { data, isLoading } = useGetAllSchemas()
  const options = useMemo<ResourceFamily[]>(() => {
    if (!data) return []
    if (!data.items) return []
    const options = getSchemaFamilies(data.items)
    return Object.values(options)
  }, [data])

  return ResourceNameCreatableSelect(props, DataHubNodeType.SCHEMA, options, createNewSchemaOption, isLoading)
}

export const ScriptNameCreatableSelect = (props: WidgetProps) => {
  const { isLoading, data } = useGetAllScripts({})

  const options = useMemo<ResourceFamily[]>(() => {
    if (!data?.items) return []
    const options = getScriptFamilies(data.items)
    return Object.values(options)
  }, [data])

  return ResourceNameCreatableSelect(props, DataHubNodeType.FUNCTION, options, createNewScriptOption, isLoading)
}
