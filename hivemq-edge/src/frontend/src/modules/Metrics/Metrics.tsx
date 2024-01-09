import { FC, useEffect } from 'react'
import {
  Accordion,
  AccordionButton,
  AccordionIcon,
  AccordionItem,
  AccordionPanel,
  Box,
  Card,
  SimpleGrid,
  useDisclosure,
  useTheme,
} from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'

import { NodeTypes } from '@/modules/Workspace/types.ts'

import config from '@/config'

import { ChartType, MetricDefinition } from './types.ts'
import useMetricsStore from './hooks/useMetricsStore.ts'
import { extractMetricInfo } from './utils/metrics-name.utils.ts'
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

// TODO[NVL] Should go to some kind of reusable routine, with verification
const defaultColorSchemes = ['blue', 'green', 'orange', 'pink', 'purple', 'red', 'teal', 'yellow', 'cyan']

const Metrics: FC<MetricsProps> = ({ nodeId, adapterIDs, initMetrics, defaultChartType }) => {
  const { t } = useTranslation()
  const { isOpen, onOpen, onClose } = useDisclosure()
  const { addMetrics, getMetricsFor, removeMetrics } = useMetricsStore()
  const { colors } = useTheme()

  const showEditor = config.features.METRICS_SHOW_EDITOR

  // TODO[NVL] Should go to some kind of reusable routine, with verification
  const colorKeys = Object.keys(colors)
  const chartTheme = defaultColorSchemes.filter((c) => colorKeys.includes(c))

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
    <>
      {showEditor && (
        <Card size={'sm'}>
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
        </Card>
      )}

      <SimpleGrid
        spacing={4}
        templateColumns="repeat(auto-fill, minmax(200px, 1fr))"
        role={'list'}
        aria-label={t('metrics.charts.list') as string}
      >
        {metrics.map((e) => {
          const { id } = extractMetricInfo(e.metrics)
          const colorSchemeIndex = adapterIDs.indexOf(id as string)

          if (!e.chart || e.chart === ChartType.SAMPLE)
            return (
              <Sample
                key={e.metrics}
                metricName={e.metrics}
                chartTheme={{ colourScheme: chartTheme[colorSchemeIndex] }}
                onClose={() => handleRemoveMetrics(e.metrics)}
                canEdit={isOpen}
                role={'listitem'}
              />
            )
          else
            return (
              <ChartContainer
                key={e.metrics}
                chartType={e.chart}
                metricName={e.metrics}
                chartTheme={{ colourScheme: chartTheme[colorSchemeIndex] }}
                onClose={() => handleRemoveMetrics(e.metrics)}
                canEdit={isOpen}
                role={'listitem'}
              />
            )
        })}
      </SimpleGrid>
    </>
  )
}

export default Metrics
