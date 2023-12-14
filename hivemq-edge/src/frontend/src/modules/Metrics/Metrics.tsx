import { FC, useEffect } from 'react'
import {
  Accordion,
  AccordionButton,
  AccordionIcon,
  AccordionItem,
  AccordionPanel,
  Box,
  Card,
  CardBody,
  SimpleGrid,
  useDisclosure,
} from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'

import { NodeTypes } from '@/modules/Workspace/types.ts'

import config from '@/config'

import { ChartType, MetricDefinition } from './types.ts'
import useMetricsStore from './hooks/useMetricsStore.ts'
import MetricEditor from './components/editor/MetricEditor.tsx'
import ChartContainer from './components/container/ChartContainer.tsx'
import Sample from './components/container/Sample.tsx'

interface MetricsProps {
  nodeId: string
  type: NodeTypes
  adapterIDs: string[]
  initMetrics?: string[]
  defaultChartType?: ChartType
}

export interface MetricSpecStorage {
  selectedTopic: string
  selectedChart?: ChartType
}

const Metrics: FC<MetricsProps> = ({ nodeId, adapterIDs, initMetrics, defaultChartType }) => {
  const { t } = useTranslation()
  const { isOpen, onOpen, onClose } = useDisclosure()
  const { addMetrics, getMetricsFor, removeMetrics } = useMetricsStore()
  const showEditor = config.features.METRICS_SHOW_EDITOR

  const handleCreateMetrics = (value: MetricDefinition) => {
    const { selectedTopic, selectedChart } = value
    addMetrics(nodeId, selectedTopic.value, selectedChart?.value)
  }

  const handleRemoveMetrics = (selectedTopic: string) => {
    removeMetrics(nodeId, selectedTopic)
  }

  const metrics = getMetricsFor(nodeId)

  useEffect(() => {
    const gg = getMetricsFor(nodeId)
    if (gg.length === 0 && initMetrics) {
      initMetrics.map((e) => {
        addMetrics(nodeId, e, defaultChartType)
      })
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  return (
    <Card size={'sm'}>
      {showEditor && (
        <Accordion
          allowToggle
          onChange={(expandedIndex) => {
            if (expandedIndex === -1) onClose()
            else onOpen()
          }}
        >
          <AccordionItem>
            <AccordionButton data-testid="metrics-toggle">
              <Box as="span" flex="1" textAlign="left">
                {t('metrics.editor.title')}
              </Box>
              <AccordionIcon />
            </AccordionButton>

            <AccordionPanel pb={4}>
              <MetricEditor
                filter={adapterIDs}
                selectedMetrics={metrics.map((e) => e.metrics)}
                selectedChart={defaultChartType}
                onSubmit={handleCreateMetrics}
              />
            </AccordionPanel>
          </AccordionItem>
        </Accordion>
      )}

      <CardBody>
        <SimpleGrid spacing={4} templateColumns="repeat(auto-fill, minmax(200px, 1fr))">
          {metrics.map((e) => {
            if (!e.chart || e.chart === ChartType.SAMPLE)
              return (
                <Sample
                  key={e.metrics}
                  metricName={e.metrics}
                  onClose={() => handleRemoveMetrics(e.metrics)}
                  canEdit={isOpen}
                />
              )
            else
              return (
                <ChartContainer
                  key={e.metrics}
                  chartType={e.chart}
                  metricName={e.metrics}
                  onClose={() => handleRemoveMetrics(e.metrics)}
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
