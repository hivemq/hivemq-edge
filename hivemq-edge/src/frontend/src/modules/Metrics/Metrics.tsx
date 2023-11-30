import { FC } from 'react'
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
  const showEditor = config.features.METRICS_SHOW_EDITOR
  const { isOpen, onOpen, onClose } = useDisclosure()
  const { t } = useTranslation()

  const handleCreateMetrics = (value: MetricDefinition) => {
    const { selectedTopic, selectedChart } = value
    setMetrics((old) => [...old, { selectedTopic: selectedTopic?.value, selectedChart: selectedChart?.value }])
  }

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
            <AccordionButton>
              <Box as="span" flex="1" textAlign="left">
                {t('metrics.editor.title')}
              </Box>
              <AccordionIcon />
            </AccordionButton>

            <AccordionPanel pb={4}>
              <MetricEditor
                filter={id}
                selectedMetrics={metrics.map((e) => e.selectedTopic)}
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
            if (!e.selectedChart || e.selectedChart === ChartType.SAMPLE)
              return (
                <Sample
                  key={e.selectedTopic}
                  metricName={e.selectedTopic}
                  onClose={() => setMetrics((old) => old.filter((x) => x.selectedTopic !== e.selectedTopic))}
                />
              )
            else
              return (
                <ChartContainer
                  key={e.selectedTopic}
                  chartType={e.selectedChart}
                  metricName={e.selectedTopic}
                  onClose={() => setMetrics((old) => old.filter((x) => x.selectedTopic !== e.selectedTopic))}
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
