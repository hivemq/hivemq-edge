import { FC } from 'react'
import { Box } from '@chakra-ui/react'

import { useGetEdgeTopics } from '@/hooks/useGetEdgeTopics/useGetEdgeTopics.ts'
import { MultiTopicsCreatableSelect, SingleTopicCreatableSelect } from '@/components/MQTT/TopicCreatableSelect.tsx'

interface SourceSelectorProps {
  topics: string[]
  multiple?: boolean
  onChange: (v: string | string[] | undefined) => void
}

const SourceSelector: FC<SourceSelectorProps> = ({ topics, onChange, multiple = false }) => {
  const { isLoading, data } = useGetEdgeTopics({
    publishOnly: true,
  })

  return (
    <Box>
      {!multiple && (
        <SingleTopicCreatableSelect
          isLoading={isLoading}
          options={data}
          id="id"
          value={topics[0]}
          onChange={onChange}
          isCreatable={true}
        />
      )}
      {multiple && (
        <MultiTopicsCreatableSelect
          isLoading={isLoading}
          id="id"
          value={topics}
          onChange={onChange}
          isCreatable={false}
        />
      )}
    </Box>
  )
}

export default SourceSelector
