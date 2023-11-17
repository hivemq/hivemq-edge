import { FC, useState } from 'react'
import { Card, CardBody, CardHeader, Flex, IconButton, SimpleGrid, useDisclosure } from '@chakra-ui/react'
import { TbLayoutNavbarExpand, TbLayoutNavbarCollapse } from 'react-icons/tb'

import { NodeTypes } from '@/modules/EdgeVisualisation/types.ts'

import config from '@/config'

import MetricNameSelector from './MetricNameSelector.tsx'
import Sample from './Sample.tsx'

interface MetricsProps {
  type: NodeTypes
  id: string
  initMetrics?: string[]
}

const Metrics: FC<MetricsProps> = ({ id, initMetrics }) => {
  const [metrics, setMetrics] = useState<string[]>(initMetrics || [])
  const showSelector = config.features.METRICS_SELECT_PANEL
  const { isOpen, onOpen, onClose } = useDisclosure()

  return (
    <Card size={'sm'} size={'sm'}>
      {showSelector && (
        <CardHeader>
          <Flex justifyContent={'flex-end'}>
            <IconButton
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
              onSubmit={(d) => {
                setMetrics((old) => [...old, d.myTopic as unknown as string])
              }}
            />
          )}
        </CardHeader>
      )}

      <CardBody>
        <SimpleGrid spacing={4} templateColumns="repeat(auto-fill, minmax(200px, 1fr))">
          {metrics.map((e) => (
            <Sample key={e} metricName={e} onClose={() => setMetrics((old) => old.filter((x) => x !== e))} />
          ))}
        </SimpleGrid>
      </CardBody>
    </Card>
  )
}

export default Metrics
