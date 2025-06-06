import { Fragment, useMemo } from 'react'
import type { WidgetProps } from '@rjsf/utils'
import { getUiOptions } from '@rjsf/utils'
import { FormControl, FormLabel } from '@chakra-ui/react'
import type { RJSFSchema } from '@rjsf/utils'

import { CustomFormat } from '@/api/types/json-schema.ts'
import { SelectTag, SelectTopic, SelectTopicFilter } from '@/components/MQTT/EntityCreatableSelect.tsx'
import type { MappingContext } from '@/modules/ProtocolAdapters/types.ts'

export const registerEntitySelectWidget =
  (type: CustomFormat) => (props: WidgetProps<WidgetProps<unknown, RJSFSchema, MappingContext>>) => {
    const { chakraProps, label, id, disabled, readonly, onChange, required, rawErrors, value } = props
    const { multiple, create } = getUiOptions(props.uiSchema)
    const { adapterId } = props.formContext

    const Select = useMemo(() => {
      if (type === CustomFormat.MQTT_TAG) return SelectTag
      if (type === CustomFormat.MQTT_TOPIC) return SelectTopic
      if (type === CustomFormat.MQTT_TOPIC_FILTER) return SelectTopicFilter
      return Fragment
    }, [])

    return (
      <FormControl
        isDisabled={disabled || readonly}
        isRequired={required}
        {...chakraProps}
        mb={1}
        isInvalid={rawErrors && rawErrors.length > 0}
      >
        <FormLabel htmlFor={id}>{label}</FormLabel>
        <Select
          adapterId={adapterId as string}
          isMulti={Boolean(multiple)}
          isCreatable={Boolean(create)}
          id={id}
          value={value}
          onChange={onChange}
        />
      </FormControl>
    )
  }
