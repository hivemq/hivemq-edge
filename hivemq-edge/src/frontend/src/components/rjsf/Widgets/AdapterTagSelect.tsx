import { ComponentType, FC, useCallback } from 'react'
import { getTemplate, labelValue, WidgetProps } from '@rjsf/utils'
import { Code, FormControl, FormLabel, HStack, Text, VStack } from '@chakra-ui/react'
import {
  chakraComponents,
  createFilter,
  GroupBase,
  OnChangeValue,
  OptionProps,
  Select,
  SingleValueProps,
} from 'chakra-react-select'
import { getChakra } from '@rjsf/chakra-ui/lib/utils'
import { RJSFSchema } from '@rjsf/utils/src/types.ts'
import { INode } from 'react-accessible-treeview'

import { useGetDataPoints } from '@/api/hooks/useProtocolAdapters/useGetDataPoints.tsx'
import { AdapterContext } from '@/modules/ProtocolAdapters/types.ts'
import { FlatObjectNode } from '@/components/rjsf/Widgets/types.ts'
import { getAdapterTreeView } from '@/components/rjsf/Widgets/utils/treeview.utils.ts'

const Option: ComponentType<OptionProps<INode<FlatObjectNode>, false, GroupBase<INode<FlatObjectNode>>>> = (props) => {
  const { metadata } = props.data
  const { description, id, name } = metadata || {}

  const [val] = props.getValue()
  return (
    <chakraComponents.Option {...props} isSelected={val?.metadata?.id === id}>
      <VStack alignItems="flex-start" width="inherit" gap={0}>
        <HStack flexWrap="nowrap" width="inherit" justifyContent="space-between">
          <Text fontWeight="bold" data-testid="dataPoint-name">
            {name}
          </Text>
          <Code data-testid="dataPoint-id">{id}</Code>
        </HStack>
        <Text fontSize="xs" data-testid="dataPoint-description">
          {description}
        </Text>
      </VStack>
    </chakraComponents.Option>
  )
}

const SingleValue = (props: SingleValueProps<INode<FlatObjectNode>>) => {
  return (
    <chakraComponents.SingleValue {...props}>
      <Text>{props.data.metadata?.id}</Text>
    </chakraComponents.SingleValue>
  )
}

const AdapterTagSelect: FC<WidgetProps<unknown, RJSFSchema, AdapterContext>> = (props) => {
  const { formContext } = props
  const { isDiscoverable, adapterType, adapterId } = formContext || {}
  if (!adapterType || !adapterId) throw new Error('The adapter has not been added to the form context')
  const { data, isLoading, isError } = useGetDataPoints(Boolean(isDiscoverable), adapterId)
  const chakraProps = getChakra({ uiSchema: props.uiSchema })

  const onChange = useCallback<(newValue: OnChangeValue<INode<FlatObjectNode>, false>) => void>(
    (newValue) => {
      if (newValue) props.onChange(newValue.metadata?.id)
    },
    [props]
  )

  if (!isDiscoverable || !adapterType || !adapterId || !data) {
    const { options, registry } = props
    const BaseInputTemplate = getTemplate<'BaseInputTemplate'>('BaseInputTemplate', registry, options)
    return <BaseInputTemplate {...props} />
  }

  const options = getAdapterTreeView(data).filter((option) => option.parent !== null)

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

      <Select<INode<FlatObjectNode>>
        isLoading={isLoading}
        isInvalid={isError}
        inputId={props.id}
        id="react-select-dataPoint-container"
        instanceId="dataPoint"
        isRequired={props.required}
        options={options}
        value={options.find((e) => e.metadata?.id === props.value)}
        onChange={onChange}
        filterOption={createFilter({
          // search in the name and description
          stringify: (option) => {
            return `${option.data.metadata?.name || ''}${option.data.metadata?.id || ''}`
          },
        })}
        components={{
          Option,
          SingleValue,
        }}
      />
    </FormControl>
  )
}

export default AdapterTagSelect
