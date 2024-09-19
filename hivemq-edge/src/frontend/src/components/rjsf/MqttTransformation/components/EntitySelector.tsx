import { FC } from 'react'

import { useGetAdapterDomainTags } from '@/api/hooks/useProtocolAdapters/useGetAdapterDomainTags.tsx'
import { MultiTopicsCreatableSelect, SingleTopicCreatableSelect } from '@/components/MQTT/TopicCreatableSelect.tsx'

// TODO[NVL] The whole topic CreatableSelect thing needs to be refactored to any type of entities
interface SourceSelectorProps {
  adapterType?: string
  adapterId?: string
  values: string[]
  onChange: (v: string | string[] | undefined) => void
}

export const SelectSourceTopics: FC<SourceSelectorProps> = ({ values, onChange }) => {
  return <MultiTopicsCreatableSelect id="mapping-select-source" value={values} onChange={onChange} isCreatable={true} />
}

export const SelectDestinationTag: FC<SourceSelectorProps> = ({ adapterId, values, onChange }) => {
  const { isLoading, data } = useGetAdapterDomainTags(adapterId)

  return (
    <SingleTopicCreatableSelect
      isLoading={isLoading}
      options={data?.items?.map((deviceTag) => deviceTag.tag) || []}
      id="mapping-select-destination"
      value={values[0]}
      onChange={onChange}
      isCreatable={false}
      isTag
    />
  )
}
