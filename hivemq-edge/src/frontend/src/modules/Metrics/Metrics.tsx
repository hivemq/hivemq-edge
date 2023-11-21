import { FC, useState } from 'react'
import { Card, CardBody, CardHeader, Flex, IconButton, SimpleGrid, useDisclosure, useToast } from '@chakra-ui/react'
import { TbLayoutNavbarExpand, TbLayoutNavbarCollapse } from 'react-icons/tb'
import { useTranslation } from 'react-i18next'

import { NodeTypes } from '@/modules/EdgeVisualisation/types.ts'

import config from '@/config'

import MetricNameSelector from './components/MetricNameSelector.tsx'
import Sample from './components/Sample.tsx'

interface MetricsProps {
  type: NodeTypes
  id: string
  initMetrics?: string[]
}

const Metrics: FC<MetricsProps> = ({ id, initMetrics }) => {
  const [metrics, setMetrics] = useState<string[]>(initMetrics || [])
  const showSelector = config.features.METRICS_SELECT_PANEL
  const { isOpen, onOpen, onClose } = useDisclosure()
  const toast = useToast()
  const { t } = useTranslation()

  const handleCopyMetric = (metricName: string, timestamp: string, value: number) => {
    const id = `${metricName}-${timestamp}`
    navigator.clipboard.writeText(JSON.stringify({ metricName, timestamp, value })).then(() => {
      if (!toast.isActive(id))
        toast({ id, duration: 3000, variant: 'subtle', description: t('metrics.command.copy.prompt') })
    })
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
              aria-label="ddf"
              fontSize={'20px'}
              icon={!isOpen ? <TbLayoutNavbarExpand /> : <TbLayoutNavbarCollapse />}
              onClick={() => (isOpen ? onClose() : onOpen())}
            />
          </Flex>
          {isOpen && (
            <MetricNameSelector
              filter={id}
              selectedMetrics={metrics}
              onSubmit={(value) => {
                const { selectedTopic } = value
                setMetrics((old) => [...old, selectedTopic.value])
              }}
            />
          )}
        </CardHeader>
      )}

      <CardBody>
        <SimpleGrid spacing={4} templateColumns="repeat(auto-fill, minmax(200px, 1fr))">
          {metrics.map((e) => (
            <Sample
              key={e}
              metricName={e}
              onClose={() => setMetrics((old) => old.filter((x) => x !== e))}
              onClipboardCopy={handleCopyMetric}
            />
          ))}
        </SimpleGrid>
      </CardBody>
    </Card>
  )
}

export default Metrics
