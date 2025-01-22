import { ComponentType, FC, useCallback, useMemo } from 'react'
import { labelValue, WidgetProps } from '@rjsf/utils'
import { getChakra } from '@rjsf/chakra-ui/lib/utils'
import { chakraComponents, OnChangeValue, OptionProps, Select } from 'chakra-react-select'
import { FormControl, FormLabel, HStack, Text } from '@chakra-ui/react'

const Option: ComponentType<OptionProps> = ({ children, ...props }) => {
  return (
    <chakraComponents.Option {...props}>
      <HStack flexWrap="nowrap">
        <Text>{children}</Text>
      </HStack>
    </chakraComponents.Option>
  )
}

export const CompactSelectWidget: FC<WidgetProps> = (props) => {
  const chakraProps = getChakra({ uiSchema: props.uiSchema })

  const options = useMemo(() => {
    if (props.options.enumOptions) return props.options.enumOptions
    else return []
  }, [props.options.enumOptions])

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const onChange = useCallback<(newValue: OnChangeValue<any, false>) => void>(
    (newValue) => {
      if (newValue) props.onChange(newValue.value)
    },
    [props]
  )

  return (
    <FormControl
      data-testid="rjsf-compact-selector"
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

      <Select
        size="md"
        inputId={props.id}
        id="react-select-dataPoint-container"
        instanceId="dataPoint"
        isRequired={props.required}
        options={options}
        value={options.find((option) => option.value === props.value)}
        onChange={onChange}
        components={{ Option }}
      />
    </FormControl>
  )
}
