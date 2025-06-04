import type { FC } from 'react'
import type { BaseInputTemplateProps } from '@rjsf/utils'
import { getUiOptions } from '@rjsf/utils'
import { FormControl, FormLabel } from '@chakra-ui/react'

import { SelectTopic } from '@/components/MQTT/EntityCreatableSelect.tsx'

export const TopicInputTemplate: FC<BaseInputTemplateProps> = (props) => {
  const { chakraProps, label, id, disabled, readonly, onChange, required, rawErrors, value } = props
  const { create, multiple } = getUiOptions(props.uiSchema)

  return (
    <FormControl
      isDisabled={disabled || readonly}
      isRequired={required}
      {...chakraProps}
      mb={1}
      isInvalid={rawErrors && rawErrors.length > 0}
    >
      <FormLabel htmlFor={id}>{label}</FormLabel>
      <SelectTopic
        isMulti={Boolean(multiple)}
        isCreatable={create === true || create === undefined}
        id={id}
        value={value}
        onChange={onChange}
      />
    </FormControl>
  )
}
