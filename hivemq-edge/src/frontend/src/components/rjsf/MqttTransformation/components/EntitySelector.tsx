import { FC } from 'react'

import { MultiTopicsCreatableSelect, SingleTopicCreatableSelect } from '@/components/MQTT/TopicCreatableSelect.tsx'
import { useGetDeviceTags } from '@/api/hooks/useTopicOntology/useGetDeviceTags.tsx'

// TODO[NVL] The whole topic CreatableSelect thing needs to be refactored to any type of entities
interface SourceSelectorProps {
  values: string[]
  onChange: (v: string | string[] | undefined) => void
}

export const SelectSourceTopics: FC<SourceSelectorProps> = ({ values, onChange }) => {
  return (
    <MultiTopicsCreatableSelect id="mapping-select-source" value={values} onChange={onChange} isCreatable={false} />
  )
}

export const SelectDestinationTag: FC<SourceSelectorProps> = ({ values, onChange }) => {
  const { isLoading, data } = useGetDeviceTags('my-tags')

  return (
    <SingleTopicCreatableSelect
      isLoading={isLoading}
      options={data?.map((deviceTag) => deviceTag.tag) || []}
      id="mapping-select-destination"
      value={values[0]}
      onChange={onChange}
      isCreatable={true}
      isTag
    />
  )
}
