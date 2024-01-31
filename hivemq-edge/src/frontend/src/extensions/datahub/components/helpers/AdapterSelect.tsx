import { FocusEvent, useCallback, useMemo } from 'react'
import { labelValue, WidgetProps } from '@rjsf/utils'
import { getChakra } from '@rjsf/chakra-ui/lib/utils'
import { FormControl, FormLabel } from '@chakra-ui/react'
import { Select, OnChangeValue } from 'chakra-react-select'

import { useListProtocolAdapters } from '@/api/hooks/useProtocolAdapters/useListProtocolAdapters.tsx'

interface AdapterType {
  label: string
  value: string
}

export const AdapterSelect = (props: WidgetProps) => {
  // TODO[NVL] This is one of the components that break boundary with the DataHub "extension". Need a better way
  const { data: adapters, isLoading, isError } = useListProtocolAdapters()

  const chakraProps = getChakra({ uiSchema: props.uiSchema })

  const onChange = useCallback<(newValue: OnChangeValue<{ label: string; value: string }, false>) => void>(
    (newValue) => {
      props.onChange(newValue?.value || undefined)
    },
    [props]
  )
  const onBlur = ({ target: { value } }: FocusEvent<HTMLInputElement>) => props.onBlur(props.id, value)
  const onFocus = ({ target: { value } }: FocusEvent<HTMLInputElement>) => props.onFocus(props.id, value)

  const options = useMemo(() => {
    if (!adapters) return []
    return adapters.map<AdapterType>((e) => ({ label: e.id, value: e.id }))
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

      <Select
        isClearable
        isLoading={isLoading}
        isInvalid={isError}
        inputId={props.id}
        isRequired={props.required}
        options={options}
        value={{ label: props.value, value: props.value }}
        onBlur={onBlur}
        onFocus={onFocus}
        onChange={onChange}
      />
    </FormControl>
  )
}
