import { FC, type MouseEvent, useState } from 'react'
import { Text } from '@chakra-ui/react'
import type { HierarchyNode } from 'd3-hierarchy'

import MetadataExplorer from '@/modules/DomainOntology/components/MetadataExplorer.tsx'
import { TopicTreeMetadata } from '@/modules/Workspace/types.ts'
import ChartWrapper from '@/modules/DomainOntology/components/parts/ChartWrapper.tsx'
import SunburstChart from '@/modules/DomainOntology/components/charts/SunburstChart.tsx'

interface TopicWheelProps {
  id?: string
  data: HierarchyNode<TopicTreeMetadata>
}

const TopicWheel: FC<TopicWheelProps> = ({ data }) => {
  const [selectedTopic, setSelectedTopic] = useState<string | undefined>()
  const isWildcard = Boolean(selectedTopic && selectedTopic.includes('#'))

  const onHandleSelect = (topic: string, event: MouseEvent) => {
    setSelectedTopic(topic)
    event.stopPropagation()
  }

  return (
    <ChartWrapper
      help={<Text>ddf</Text>}
      footer={selectedTopic && !isWildcard && <MetadataExplorer topic={selectedTopic} />}
    >
      <SunburstChart data={data} onSelect={onHandleSelect} />
    </ChartWrapper>
  )
}

export default TopicWheel
