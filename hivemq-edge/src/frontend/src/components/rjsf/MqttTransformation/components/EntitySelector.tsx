import { FC } from 'react'
import { useTranslation } from 'react-i18next'

import { useGetDomainTags } from '@/api/hooks/useProtocolAdapters/useGetDomainTags.tsx'
import { SingleTopicCreatableSelect } from '@/components/MQTT/TopicCreatableSelect.tsx'
import { useGetEdgeTopics } from '@/hooks/useGetEdgeTopics/useGetEdgeTopics.ts'

// TODO[NVL] The whole topic CreatableSelect thing needs to be refactored to any type of entities
interface SourceSelectorProps {
  adapterType?: string
  adapterId?: string
  value: string | undefined
  onChange: (v: string | string[] | undefined) => void
}

export const SelectSourceTopics: FC<SourceSelectorProps> = ({ value, onChange }) => {
  const { data, isLoading } = useGetEdgeTopics({ publishOnly: false })
  const { t } = useTranslation('components')

  return (
    <SingleTopicCreatableSelect
      placeholder={t('rjsf.MqttTransformationField.sources.selector.placeholder')}
      noOptionsMessage={() => t('rjsf.MqttTransformationField.sources.selector.noOptionsMessage')}
      isLoading={isLoading}
      options={data}
      id="mapping-select-source"
      value={value || ''}
      onChange={onChange}
      isCreatable={false}
      isTag
    />
  )
}

export const SelectDestinationTag: FC<SourceSelectorProps> = ({ adapterId, value, onChange }) => {
  const { t } = useTranslation('components')
  const { isLoading, data } = useGetDomainTags(adapterId)

  return (
    <SingleTopicCreatableSelect
      placeholder={t('rjsf.MqttTransformationField.destination.selector.placeholder')}
      noOptionsMessage={() => t('rjsf.MqttTransformationField.destination.selector.noOptionsMessage')}
      isLoading={isLoading}
      options={data?.items?.map((deviceTag) => deviceTag.tag) || []}
      id="mapping-select-destination"
      value={value || ''}
      onChange={onChange}
      isCreatable={false}
      isTag
    />
  )
}
