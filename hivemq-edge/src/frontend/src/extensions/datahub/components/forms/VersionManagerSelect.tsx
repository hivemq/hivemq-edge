import type { FocusEvent } from 'react'
import { useCallback, useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import type { WidgetProps } from '@rjsf/utils'
import { labelValue } from '@rjsf/utils'
import { getChakra } from '@rjsf/chakra-ui/lib/utils'
import type { OnChangeValue } from 'chakra-react-select'
import { Select } from 'chakra-react-select'
import { FormControl, FormLabel } from '@chakra-ui/react'

import { ResourceStatus, ResourceWorkingVersion } from '@datahub/types.ts'

interface VersionOption {
  label: string
  value: number
  isLatest?: boolean
}

export const VersionManagerSelect = (props: WidgetProps) => {
  const { t } = useTranslation('datahub')
  const chakraProps = getChakra({ uiSchema: props.uiSchema })
  const { options } = props

  const selectOptions = useMemo(() => {
    const internalVersions = options.selectOptions as number[] | undefined
    if (props.value === ResourceWorkingVersion.MODIFIED || props.value === ResourceWorkingVersion.DRAFT) {
      return [
        {
          label: t('workspace.version.status', {
            context: props.value === ResourceWorkingVersion.MODIFIED ? ResourceStatus.MODIFIED : 'DRAFT',
          }),
          value: props.value,
        },
      ]
    }
    if (!internalVersions) return []

    return internalVersions.map<VersionOption>((versionNumber, index, row) => {
      return {
        label: t('workspace.version.display', { versionNumber, count: index + 1 === row.length ? 0 : 1 }),
        value: versionNumber,
        isLatest: index + 1 === row.length,
      }
    })
  }, [options.selectOptions, props.value, t])

  const onChange = useCallback<(newValue: OnChangeValue<VersionOption, false>) => void>(
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

      <Select<VersionOption>
        inputId={props.id}
        isRequired={props.required}
        options={selectOptions}
        value={selectOptions.find((e) => e.value === props.value)}
        onBlur={onBlur}
        onFocus={onFocus}
        onChange={onChange}
      />
    </FormControl>
  )
}
