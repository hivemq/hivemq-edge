import { FC, type MouseEvent, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Text } from '@chakra-ui/react'

import ErrorMessage from '@/components/ErrorMessage.tsx'
import MetadataExplorer from '@/modules/DomainOntology/components/MetadataExplorer.tsx'
import ChartWrapper from '@/modules/DomainOntology/components/parts/ChartWrapper.tsx'
import SunburstChart from '@/modules/DomainOntology/components/charts/SunburstChart.tsx'
import { useGetSunburstData } from '@/modules/DomainOntology/hooks/useGetSunburstData.ts'

const ConceptWheel: FC = () => {
  const { t } = useTranslation()
  const { sunburstData, isError } = useGetSunburstData()
  const [selectedTopic, setSelectedTopic] = useState<string | undefined>()
  const isWildcard = Boolean(selectedTopic && selectedTopic.includes('#'))

  const onHandleSelect = (topic: string, event: MouseEvent) => {
    setSelectedTopic(topic)
    event.stopPropagation()
  }

  if (isError) return <ErrorMessage type={t('ontology.error.loading')} />

  return (
    <ChartWrapper
      help={<Text>ddf</Text>}
      footer={selectedTopic && !isWildcard && <MetadataExplorer topic={selectedTopic} />}
    >
      <SunburstChart data={sunburstData} onSelect={onHandleSelect} />
    </ChartWrapper>
  )
}

export default ConceptWheel
