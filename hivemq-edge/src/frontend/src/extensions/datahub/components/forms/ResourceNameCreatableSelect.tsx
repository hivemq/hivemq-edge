import { useCallback } from 'react'
import {
  ActionMeta,
  chakraComponents,
  CreatableSelect,
  createFilter,
  OnChangeValue,
  OptionProps,
  Options,
  SingleValueProps,
} from 'chakra-react-select'
import { getChakra } from '@rjsf/chakra-ui/lib/utils'
import { labelValue, WidgetProps } from '@rjsf/utils'
import { FormControl, FormLabel, HStack, Text, VStack } from '@chakra-ui/react'

import { DataHubNodeType, ResourceFamily } from '@datahub/types.ts'
import { useGetAllSchemas } from '@datahub/api/hooks/DataHubSchemasService/useGetAllSchemas.tsx'
import { useGetAllScripts } from '@datahub/api/hooks/DataHubScriptsService/useGetAllScripts.tsx'
import { getSchemaFamilies } from '@datahub/designer/schema/SchemaNode.utils.ts'
import { useTranslation } from 'react-i18next'

const SingleValue = <T extends ResourceFamily>(props: SingleValueProps<T>) => {
  return (
    <chakraComponents.SingleValue {...props}>
      <Text>{props.data.name || props.data.label}</Text>
    </chakraComponents.SingleValue>
  )
}

const Option = <T extends ResourceFamily>(props: OptionProps<T>) => {
  const { isSelected, ...rest } = props
  const [selectedOption] = props.getValue()

  // @ts-ignore
  const { __isNew__ } = props.data
  if (__isNew__) {
    return <chakraComponents.Option {...props}>{props.children}</chakraComponents.Option>
  }

  return (
    <chakraComponents.Option {...rest} isSelected={selectedOption && selectedOption.name === props.data.name}>
      <VStack w="100%" alignItems="stretch" gap={0}>
        <HStack>
          <Text as="b" flex={1}>
            {props.data.name}
          </Text>
          <HStack>
            <Text fontSize="sm">{props.data.type}</Text>
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
  options: Options<ResourceFamily> | undefined,
  isLoading: boolean
) => {
  const { t } = useTranslation('datahub')
  const chakraProps = getChakra({ uiSchema: props.uiSchema })

  const onCreatableSelectChange = useCallback<
    (newValue: OnChangeValue<ResourceFamily, false>, actionMeta: ActionMeta<ResourceFamily>) => void
  >(
    (newValue, actionMeta) => {
      if (actionMeta.action === 'select-option' && newValue) props.onChange(newValue.name)
      if (actionMeta.action === 'create-option' && newValue) {
        props.onChange(newValue.label)
      }
    },
    [props]
  )

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
      />
    </FormControl>
  )
}

export const SchemaNameCreatableSelect = (props: WidgetProps) => {
  const { data, isLoading } = useGetAllSchemas()
  const options = getSchemaFamilies(data?.items || [])
  return ResourceNameCreatableSelect(props, DataHubNodeType.SCHEMA, Object.values(options), isLoading)
}

export const ScriptNameCreatableSelect = (props: WidgetProps) => {
  const { isLoading } = useGetAllScripts({})

  // TODO[NVL] Don't forget to convert the scripts
  return ResourceNameCreatableSelect(props, DataHubNodeType.FUNCTION, [], isLoading)
}
