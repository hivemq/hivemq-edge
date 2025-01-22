import { FC, type MouseEvent, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Text } from '@chakra-ui/react'

import ErrorMessage from '@/components/ErrorMessage.tsx'
import LoaderSpinner from '@/components/Chakra/LoaderSpinner.tsx'
import MetadataExplorer from '@/modules/DomainOntology/components/MetadataExplorer.tsx'
import ChartWrapper from '@/modules/DomainOntology/components/parts/ChartWrapper.tsx'
import SunburstChart from '@/modules/DomainOntology/components/charts/SunburstChart.tsx'
import { useGetSunburstData } from '@/modules/DomainOntology/hooks/useGetSunburstData.ts'

const ConceptWheel: FC = () => {
  const { t } = useTranslation()
  const { sunburstData, isError, isLoading } = useGetSunburstData()
  const [selectedTopic, setSelectedTopic] = useState<string | undefined>()
  const isWildcard = Boolean(selectedTopic && selectedTopic.includes('#'))

  const arrayHelpStrings = t('ontology.charts.conceptWheel.help', { returnObjects: true }) as unknown as string[]

  const onHandleSelect = (topic: string, event: MouseEvent) => {
    setSelectedTopic(topic)
    event.stopPropagation()
  }

  if (isLoading) return <LoaderSpinner />
  if (isError) return <ErrorMessage type={t('ontology.error.loading')} />

  return (
    <ChartWrapper
      data-testid="edge-panel-concept-wheel"
      helpTitle={t('ontology.charts.conceptWheel.title')}
      help={arrayHelpStrings.map((line, index) => (
        <Text key={`test-${index}`}>{line}</Text>
      ))}
      footer={selectedTopic && !isWildcard && <MetadataExplorer topic={selectedTopic} />}
    >
      <SunburstChart data={sunburstData} onSelect={onHandleSelect} />
    </ChartWrapper>
  )
}

export default ConceptWheel
