import { FC } from 'react'
import { Box } from '@chakra-ui/react'

import { useGetEdgeTopics } from '@/hooks/useGetEdgeTopics/useGetEdgeTopics.ts'
import { MultiTopicsCreatableSelect, SingleTopicCreatableSelect } from '@/components/MQTT/TopicCreatableSelect.tsx'
import { useGetDeviceTags } from '@/api/hooks/useTopicOntology/useGetDeviceTags.tsx'

interface SourceSelectorProps {
  isTag?: boolean
  topics: string[]
  multiple?: boolean
  onChange: (v: string | string[] | undefined) => void
}

const SourceSelector: FC<SourceSelectorProps> = ({ topics, onChange, multiple = false, isTag = false }) => {
  const { isLoading, data } = useGetEdgeTopics({
    publishOnly: true,
  })
  const { isLoading: isTagsLoading, data: tags } = useGetDeviceTags(isTag ? 'my-tags' : undefined)

  return (
    <Box>
      {!multiple && (
        <SingleTopicCreatableSelect
          isLoading={isLoading || isTagsLoading}
          options={isTag ? tags?.map((e) => e.tag) || [] : data}
          id="id"
          value={topics[0]}
          onChange={onChange}
          isCreatable={true}
          isTag={isTag}
        />
      )}
      {multiple && (
        <MultiTopicsCreatableSelect
          isLoading={isLoading}
          id="id"
          value={topics}
          onChange={onChange}
          isCreatable={false}
          isTag={isTag}
        />
      )}
    </Box>
  )
}

export default SourceSelector
