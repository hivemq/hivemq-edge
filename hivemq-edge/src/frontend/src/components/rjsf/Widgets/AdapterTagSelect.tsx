import type { ComponentType, FC } from 'react'
import { useCallback } from 'react'
import { useTranslation } from 'react-i18next'
import type { WidgetProps } from '@rjsf/utils'
import { getTemplate, labelValue } from '@rjsf/utils'
import { getChakra } from '@rjsf/chakra-ui/lib/utils'
import type { RJSFSchema } from '@rjsf/utils/src/types.ts'
import { Breadcrumb, BreadcrumbItem, Code, FormControl, FormLabel, HStack, Text, VStack } from '@chakra-ui/react'
import type { GroupBase, OnChangeValue, OptionProps, SingleValueProps } from 'chakra-react-select'
import { chakraComponents, createFilter, Select } from 'chakra-react-select'

import { useGetDataPoints } from '@/api/hooks/useProtocolAdapters/useGetDataPoints.ts'
import type { FlatObjectNode, INode } from '@/components/rjsf/Widgets/types.ts'
import { getAdapterTreeView } from '@/components/rjsf/Widgets/utils/treeview.utils.ts'
import type { AdapterContext } from '@/modules/ProtocolAdapters/types.ts'

const Option: ComponentType<OptionProps<INode<FlatObjectNode>, false, GroupBase<INode<FlatObjectNode>>>> = (props) => {
  const { metadata } = props.data
  const { description, id, name } = metadata || {}

  const [val] = props.getValue()
  return (
    <chakraComponents.Option {...props} isSelected={val?.metadata?.value === id}>
      <VStack alignItems="flex-start" width="inherit" gap={0}>
        <HStack flexWrap="nowrap" width="inherit" justifyContent="space-between">
          <Text fontWeight="bold" data-testid="dataPoint-name">
            {name}
          </Text>
          <Code data-testid="dataPoint-id">{id}</Code>
        </HStack>
        <VStack width="inherit" alignItems="flex-start" gap={0} pl={4}>
          <Breadcrumb separator=">" fontSize="xs">
            {metadata?.breadcrumb?.slice(0, -1).map((crumb, index) => (
              <BreadcrumbItem key={`${crumb}-${index}`}>
                <Text>{crumb}</Text>
              </BreadcrumbItem>
            ))}
          </Breadcrumb>
          <Text fontSize="xs" data-testid="dataPoint-description">
            {description}
          </Text>
        </VStack>
      </VStack>
    </chakraComponents.Option>
  )
}

const SingleValue = (props: SingleValueProps<INode<FlatObjectNode>>) => {
  return (
    <chakraComponents.SingleValue {...props}>
      <Text>{props.data.metadata?.value}</Text>
    </chakraComponents.SingleValue>
  )
}

const AdapterTagSelect: FC<WidgetProps<unknown, RJSFSchema, AdapterContext>> = (props) => {
  const { t } = useTranslation('components')
  const { formContext } = props
  const { isDiscoverable, adapterType, adapterId } = formContext || {}
  const { data, isLoading, isError } = useGetDataPoints(Boolean(isDiscoverable), adapterId)
  const chakraProps = getChakra({ uiSchema: props.uiSchema })

  const onChange = useCallback<(newValue: OnChangeValue<INode<FlatObjectNode>, false>) => void>(
    (newValue) => {
      if (newValue) props.onChange(newValue.metadata?.value)
    },
    [props]
  )

  if (!isDiscoverable || !adapterType || !adapterId || !data) {
    const { options, registry } = props
    const BaseInputTemplate = getTemplate<'BaseInputTemplate'>('BaseInputTemplate', registry, options)
    return <BaseInputTemplate {...props} />
  }

  const flattenDataTree = getAdapterTreeView(data)
  const options = flattenDataTree.filter((option) => option.parent !== null && option.metadata?.selectable === true)

  function noOptionsMessage(obj: { inputValue: string }) {
    const getErrorMessage = () => {
      if (!flattenDataTree.length) return t('rjsf.AdapterTagSelect.select.noTags')
      if (!options.length) return t('rjsf.AdapterTagSelect.select.noSelectable')

      return t('rjsf.AdapterTagSelect.select.noOption', { count: obj.inputValue.length, value: obj.inputValue })
    }

    return <Text>{getErrorMessage()}</Text>
  }

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
        value={options.find((e) => e.metadata?.value === props.value)}
        onChange={onChange}
        noOptionsMessage={noOptionsMessage}
        filterOption={createFilter({
          stringify: (option) => {
            return `${option.data.metadata?.name || ''}${option.data.metadata?.value || ''}`
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
