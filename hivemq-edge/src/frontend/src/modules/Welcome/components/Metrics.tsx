import { FC, useState } from 'react'
import { Card, CardBody, CardHeader, SimpleGrid } from '@chakra-ui/react'

import MetricNameSelector from './MetricNameSelector.tsx'
import Sample from './Sample.tsx'
import config from '@/config'

interface MetricsProps {
  id?: string
}

const Metrics: FC<MetricsProps> = () => {
  const [metrics, setMetrics] = useState<string[]>(config.features.METRICS_DEFAULTS)
  const showSelector = config.features.METRICS_SELECT_PANEL

  return (
    <Card flex={1}>
      {showSelector && (
        <CardHeader>
          <MetricNameSelector
            selectedMetrics={metrics}
            onSubmit={(d) => {
              setMetrics((old) => [...old, d.myTopic])
            }}
          />
        </CardHeader>
      )}

      <CardBody>
        <SimpleGrid
          spacing={4}
          templateColumns="repeat(auto-fill, minmax(200px, 1fr))"
          overflowY={'auto'}
          maxHeight={'30vh'}
          tabIndex={0}
        >
          {metrics.map((e) => (
            <Sample key={e} metricName={e} />
          ))}
        </SimpleGrid>
      </CardBody>
    </Card>
  )
}

export default Metrics
