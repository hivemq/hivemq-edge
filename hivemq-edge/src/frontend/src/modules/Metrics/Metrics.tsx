import { FC, useState } from 'react'
import { Card, CardBody, CardHeader, Flex, IconButton, SimpleGrid, useDisclosure } from '@chakra-ui/react'
import { TbLayoutNavbarCollapse, TbLayoutNavbarExpand } from 'react-icons/tb'
import { useTranslation } from 'react-i18next'

import { NodeTypes } from '@/modules/EdgeVisualisation/types.ts'

import config from '@/config'

import { ChartType, MetricDefinition } from './types.ts'
import MetricEditor from './components/editor/MetricEditor.tsx'
import ChartContainer from './components/container/ChartContainer.tsx'
import Sample from './components/container/Sample.tsx'

interface MetricsProps {
  type: NodeTypes
  id: string
  initMetrics?: string[]
  defaultChartType?: ChartType
}

export interface MetricSpecStorage {
  selectedTopic: string
  selectedChart?: ChartType
}

const Metrics: FC<MetricsProps> = ({ id, initMetrics, defaultChartType }) => {
  // const [, saveReport] = useLocalStorage<MetricVisualisation[]>(`reports-${id}`, [])
  const [metrics, setMetrics] = useState<MetricSpecStorage[]>(
    initMetrics ? initMetrics.map<MetricSpecStorage>((e) => ({ selectedTopic: e })) : []
  )
  const showSelector = config.features.METRICS_SELECT_PANEL
  const { isOpen, onOpen, onClose } = useDisclosure()
  const { t } = useTranslation()

  const handleCreateMetrics = (value: MetricDefinition) => {
    const { selectedTopic, selectedChart } = value
    setMetrics((old) => [...old, { selectedTopic: selectedTopic?.value, selectedChart: selectedChart?.value }])
  }

  return (
    <Card size={'sm'}>
      {showSelector && (
        <CardHeader>
          <Flex justifyContent={'flex-end'}>
            <IconButton
              data-testid="metrics-toggle"
              variant={'ghost'}
              size={'sm'}
              aria-label={t('metrics.command.showSelector.ariaLabel')}
              fontSize={'20px'}
              icon={!isOpen ? <TbLayoutNavbarExpand /> : <TbLayoutNavbarCollapse />}
              onClick={() => (isOpen ? onClose() : onOpen())}
            />
          </Flex>
          {isOpen && (
            <>
              <MetricEditor
                filter={id}
                selectedMetrics={metrics.map((e) => e.selectedTopic)}
                selectedChart={defaultChartType}
                onSubmit={handleCreateMetrics}
              />
            </>
          )}
        </CardHeader>
      )}

      <CardBody>
        <SimpleGrid spacing={4} templateColumns="repeat(auto-fill, minmax(200px, 1fr))">
          {metrics.map((e) => {
            if (!e.selectedChart || e.selectedChart === ChartType.SAMPLE)
              return (
                <Sample
                  key={e.selectedTopic}
                  metricName={e.selectedTopic}
                  onClose={() => setMetrics((old) => old.filter((x) => x !== e))}
                />
              )
            else
              return (
                <ChartContainer
                  key={e.selectedTopic}
                  chartType={e.selectedChart}
                  metricName={e.selectedTopic}
                  onClose={() => setMetrics((old) => old.filter((x) => x !== e))}
                  canEdit={isOpen}
                />
              )
          })}
        </SimpleGrid>
      </CardBody>
    </Card>
  )
}

export default Metrics
