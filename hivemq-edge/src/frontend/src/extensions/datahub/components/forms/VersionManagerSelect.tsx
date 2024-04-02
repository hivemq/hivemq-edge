import { FocusEvent, useCallback, useMemo } from 'react'
import { labelValue, WidgetProps } from '@rjsf/utils'
import { getChakra } from '@rjsf/chakra-ui/lib/utils'
import { OnChangeValue, Select } from 'chakra-react-select'
import { useTranslation } from 'react-i18next'
import { FormControl, FormLabel } from '@chakra-ui/react'
import { ResourceStatus } from '@datahub/types.ts'

export const VersionManagerSelect = (props: WidgetProps) => {
  const { t } = useTranslation('datahub')
  const chakraProps = getChakra({ uiSchema: props.uiSchema })
  const { options } = props

  const selectOptions = useMemo(() => {
    const internalVersions = options.selectOptions as number[] | undefined
    if (!internalVersions) return []
    return internalVersions.map((versionNumber, index, row) => ({
      label: t('workspace.version.display', { versionNumber, count: index + 1 === row.length ? 0 : 1 }),
      value: versionNumber.toString(),
    }))
  }, [t, options.selectOptions])

  const label = useMemo(() => {
    if (props.value === ResourceStatus.MODIFIED || props.value === ResourceStatus.DRAFT)
      return t('workspace.version.status', { context: props.value })
    return props.value.toString()
  }, [props.value, t])

  const onChange = useCallback<(newValue: OnChangeValue<{ label: string; value: string }, false>) => void>(
    (newValue) => {
      if (newValue) props.onChange(newValue.value)
    },
    [props]
  )

  const onBlur = ({ target: { value } }: FocusEvent<HTMLInputElement>) => props.onBlur(props.id, value)
  const onFocus = ({ target: { value } }: FocusEvent<HTMLInputElement>) => props.onFocus(props.id, value)

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
        inputId={props.id}
        isRequired={props.required}
        options={selectOptions}
        value={{ label: label, value: props.value }}
        onBlur={onBlur}
        onFocus={onFocus}
        onChange={onChange}
      />
    </FormControl>
  )
}
