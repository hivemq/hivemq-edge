import { FC } from 'react'
import { BaseInputTemplateProps, getUiOptions } from '@rjsf/utils'
import { FormControl, FormLabel } from '@chakra-ui/react'

import { MultiTopicsCreatableSelect, SingleTopicCreatableSelect } from '@/components/MQTT/TopicCreatableSelect.tsx'
import { useGetEdgeTopics } from '@/hooks/useGetEdgeTopics/useGetEdgeTopics.ts'

import config from '@/config'

export const TopicInputTemplate: FC<BaseInputTemplateProps> = (props) => {
  const { chakraProps, label, id, disabled, readonly, onChange, required, rawErrors, value } = props
  const { isLoading, data } = useGetEdgeTopics({
    branchOnly: config.features.TOPIC_EDITOR_SHOW_BRANCHES,
    publishOnly: true,
  })
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
      {!multiple && (
        <SingleTopicCreatableSelect isLoading={isLoading} options={data} id={id} value={value} onChange={onChange} />
      )}
      {multiple && (
        <MultiTopicsCreatableSelect
          isLoading={isLoading}
          id={id}
          value={value}
          onChange={onChange}
          isCreatable={Boolean(create)}
        />
      )}
    </FormControl>
  )
}
