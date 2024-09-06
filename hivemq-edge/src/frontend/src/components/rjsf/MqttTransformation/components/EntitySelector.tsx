import { FC } from 'react'
import { Box } from '@chakra-ui/react'

import { useGetEdgeTopics } from '@/hooks/useGetEdgeTopics/useGetEdgeTopics.ts'
import { MultiTopicsCreatableSelect, SingleTopicCreatableSelect } from '@/components/MQTT/TopicCreatableSelect.tsx'
import { useGetDeviceTags } from '@/api/hooks/useTopicOntology/useGetDeviceTags.tsx'

interface SourceSelectorProps {
  type: 'topic' | 'tag'
  topics: string[]
  multiple?: boolean
  onChange: (v: string | string[] | undefined) => void
}

const EntitySelector: FC<SourceSelectorProps> = ({ topics, onChange, multiple = false, type }) => {
  const { isLoading, data } = useGetEdgeTopics({
    publishOnly: true,
  })
  const { isLoading: isTagsLoading, data: tags } = useGetDeviceTags(type === 'tag' ? 'my-tags' : undefined)

  return (
    <Box>
      {!multiple && (
        <SingleTopicCreatableSelect
          isLoading={isLoading || isTagsLoading}
          options={type === 'tag' ? tags?.map((deviceTag) => deviceTag.tag) || [] : data}
          id="id"
          value={topics[0]}
          onChange={onChange}
          isCreatable={true}
          isTag={type === 'tag'}
        />
      )}
      {multiple && (
        <MultiTopicsCreatableSelect
          isLoading={isLoading}
          id="id"
          value={topics}
          onChange={onChange}
          isCreatable={false}
          isTag={type === 'tag'}
        />
      )}
    </Box>
  )
}

export default EntitySelector
