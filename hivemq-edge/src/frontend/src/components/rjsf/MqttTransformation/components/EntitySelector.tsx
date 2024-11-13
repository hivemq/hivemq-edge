import { FC } from 'react'

import { SelectEntityType, SelectTag, SelectTopicFilter } from '@/components/MQTT/EntityCreatableSelect.tsx'
import { FormControl, FormLabel } from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'

interface SourceSelectorProps {
  adapterType?: string
  adapterId?: string
  value: string | undefined
  onChange: (v: string | readonly string[] | null) => void
}

export const SelectSourceTopics: FC<SourceSelectorProps> = ({ value, onChange }) => {
  const { t } = useTranslation('components')
  return (
    <FormControl isRequired isInvalid={!value}>
      <FormLabel htmlFor={`react-select-${SelectEntityType.TOPIC_FILTER}-input`}>
        {t('rjsf.MqttTransformationField.sources.selector.formLabel')}
      </FormLabel>
      <SelectTopicFilter value={value || null} onChange={onChange} id="mapping-select-source" />{' '}
    </FormControl>
  )
}

export const SelectDestinationTag: FC<SourceSelectorProps> = ({ adapterId, value, onChange }) => {
  const { t } = useTranslation('components')
  return (
    <FormControl isRequired isInvalid={!value}>
      <FormLabel htmlFor={`react-select-${SelectEntityType.TAG}-input`}>
        {t('rjsf.MqttTransformationField.destination.selector.formLabel')}
      </FormLabel>
      <SelectTag
        adapterId={adapterId as string}
        id="mapping-select-destination"
        onChange={onChange}
        value={value || null}
      />
    </FormControl>
  )
}
