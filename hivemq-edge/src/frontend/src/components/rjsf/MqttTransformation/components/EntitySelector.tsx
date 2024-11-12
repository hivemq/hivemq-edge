import { FC } from 'react'

import { SelectTag, SelectTopicFilter } from '@/components/MQTT/EntityCreatableSelect.tsx'

interface SourceSelectorProps {
  adapterType?: string
  adapterId?: string
  value: string | undefined
  onChange: (v: string | readonly string[] | null) => void
}

export const SelectSourceTopics: FC<SourceSelectorProps> = ({ value, onChange }) => {
  return <SelectTopicFilter value={value || null} onChange={onChange} id="mapping-select-source" />
}

export const SelectDestinationTag: FC<SourceSelectorProps> = ({ adapterId, value, onChange }) => {
  return (
    <SelectTag
      adapterId={adapterId as string}
      id="mapping-select-destination"
      onChange={onChange}
      value={value || null}
    />
  )
}
