import { labelValue, WidgetProps } from '@rjsf/utils'
import { getChakra } from '@rjsf/chakra-ui/lib/utils'
import { FormControl, FormLabel } from '@chakra-ui/react'
import { CreatableSelect } from 'chakra-react-select'
import { useTranslation } from 'react-i18next'

export const VersionManagerSelect = (props: WidgetProps) => {
  const { t } = useTranslation('datahub')
  const chakraProps = getChakra({ uiSchema: props.uiSchema })

  const options = [
    { label: 'v0.0', value: 'v0.0' },
    { label: props.value, value: props.value },
  ]

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

      <CreatableSelect
        inputId={props.id}
        isRequired={props.required}
        options={options}
        formatCreateLabel={(value) => t('workspace.version.create', { newVersion: value, oldVersion: props.value })}
        value={{ label: props.value, value: props.value }}
      />
    </FormControl>
  )
}
